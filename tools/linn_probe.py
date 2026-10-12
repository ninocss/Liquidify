"""Read-only SSDP/OpenHome feasibility inventory. Never changes queue or credentials."""
from __future__ import annotations
import argparse
import datetime
import ipaddress
import json
import socket
import time
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

LIMIT = 1024 * 1024


def local_url(url):
    parsed = urllib.parse.urlsplit(url)
    if parsed.scheme != 'http' or parsed.username or parsed.password or not parsed.hostname:
        raise ValueError('Expected local HTTP device URL without credentials')
    addresses = socket.getaddrinfo(parsed.hostname, parsed.port or 80, type=socket.SOCK_STREAM)
    if not addresses or any(not (ipaddress.ip_address(a[4][0]).is_private and
                                not ipaddress.ip_address(a[4][0]).is_loopback) for a in addresses):
        raise ValueError('Device URL must resolve only to private LAN addresses')
    return url


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        raise ValueError('Device HTTP redirects are not accepted')


def fetch(url, data=None, headers=None, timeout=3):
    request = urllib.request.Request(local_url(url), data=data, headers=headers or {})
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}), NoRedirect())
    with opener.open(request, timeout=timeout) as response:
        body = response.read(LIMIT + 1)
    if len(body) > LIMIT:
        raise ValueError('Device response exceeds size limit')
    if b'<!DOCTYPE' in body.upper() or b'<!ENTITY' in body.upper():
        raise ValueError('DTD/entity declarations are not accepted')
    return ET.fromstring(body)


def headers(packet):
    lines = packet.decode('iso-8859-1').split('\r\n')
    if not lines[0].startswith('HTTP/1.1 200'):
        return {}
    return {k.strip().lower(): v.strip() for line in lines[1:] if ':' in line
            for k, v in [line.split(':', 1)]}


def discover(seconds=5, interface='0.0.0.0'):
    found = {}
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sock:
        sock.bind((interface, 0))
        sock.setsockopt(socket.IPPROTO_IP, socket.IP_MULTICAST_TTL, 2)
        if interface != '0.0.0.0':
            sock.setsockopt(socket.IPPROTO_IP, socket.IP_MULTICAST_IF, socket.inet_aton(interface))
        for target in ('urn:av-openhome-org:service:Playlist:1', 'upnp:rootdevice'):
            packet = ('M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\n'
                      'MAN: "ssdp:discover"\r\nMX: 2\r\nST: ' + target + '\r\n\r\n').encode()
            sock.sendto(packet, ('239.255.255.250', 1900))
        deadline = time.monotonic() + seconds
        while time.monotonic() < deadline:
            sock.settimeout(max(.01, deadline - time.monotonic()))
            try:
                packet, address = sock.recvfrom(65507)
            except socket.timeout:
                break
            item = headers(packet)
            if item.get('location') and item.get('usn'):
                # UUID is stable across services and changing IP addresses.
                found[item['usn'].split('::')[0]] = dict(location=item['location'], address=address[0])
    return found


def value(node, name):
    return node.findtext('{*}' + name, default='')


def soap(url, service, action):
    payload = ('<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" '
               's:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">'
               '<s:Body><u:' + action + ' xmlns:u="' + service + '"/></s:Body></s:Envelope>').encode()
    root = fetch(url, payload, {'Content-Type': 'text/xml; charset="utf-8"',
                               'SOAPACTION': '"' + service + '#' + action + '"'})
    response = root.find('.//{*}' + action + 'Response')
    if response is None:
        raise ValueError('Missing SOAP action response')
    return {child.tag.split('}')[-1]: child.text or '' for child in response}


def inventory(location):
    root = fetch(location)
    base = value(root, 'URLBase') or location
    # Validate URLBase too, even before resolving service endpoints.
    local_url(base)
    devices = []
    for node in root.findall('.//{*}device'):
        services = []
        for service in node.findall('./{*}serviceList/{*}service'):
            kind = value(service, 'serviceType')
            control = urllib.parse.urljoin(base, value(service, 'controlURL'))
            scpd = urllib.parse.urljoin(base, value(service, 'SCPDURL'))
            entry = dict(type=kind, controlURL=control, eventURL=urllib.parse.urljoin(base, value(service, 'eventSubURL')))
            try:
                description = fetch(scpd)
                actions = [value(a, 'name') for a in description.findall('.//{*}action')]
                entry['actions'] = actions
                # Only documented, argument-free read actions. Never read Credentials.
                reads = ('TracksMax', 'ProtocolInfo') if ':Playlist:' in kind else ()
                if ':Product:' in kind:
                    reads = ('Manufacturer', 'Model', 'Product', 'Attributes')
                if ':Volkano:' in kind:
                    reads = ('SoftwareVersion',)
                if ':OAuth:' in kind:
                    reads = ('GetSupportedServices',)
                for action in reads:
                    if action in actions:
                        try:
                            entry[action] = soap(control, kind, action)
                        except Exception as error:
                            entry[action] = {'error': str(error)}
            except Exception as error:
                entry['error'] = str(error)
            services.append(entry)
        devices.append(dict(uuid=value(node, 'UDN'), name=value(node, 'friendlyName'),
                            manufacturer=value(node, 'manufacturer'), model=value(node, 'modelName'),
                            modelNumber=value(node, 'modelNumber'), services=services))
    return devices


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--location', help='Exact SSDP device description URL; no assumed Linn endpoint')
    parser.add_argument('--interface', default='0.0.0.0', help='Local IPv4 interface for multicast')
    parser.add_argument('--seconds', type=float, default=5)
    parser.add_argument('--report', type=Path, default=Path('research/linn-inventory.json'))
    args = parser.parse_args()
    report = dict(checkedAt=datetime.datetime.now(datetime.timezone.utc).isoformat(),
                  mode='read-only', devices=[], errors=[], hardwarePlaybackVerified=False,
                  tidalConnect='not determined by SSDP', firmware='not yet verified',
                  remaining=['Linn TIDAL authentication and current track reference',
                             'audible playback and autonomous track changes after controller exit'])
    try:
        locations = [args.location] if args.location else [d['location'] for d in discover(args.seconds, args.interface).values()]
        for location in dict.fromkeys(locations):
            try:
                report['devices'].extend(inventory(location))
            except Exception as error:
                report['errors'].append(dict(location=location, error=str(error)))
    except Exception as error:
        report['errors'].append(dict(error=str(error)))
    for device in report['devices']:
        if device['model'] == 'Majik DSM':
            for service in device['services']:
                version = service.get('SoftwareVersion', {}).get('aSoftwareVersion')
                if version:
                    report['firmware'] = version
    report['status'] = 'inventory only; playback gate closed' if report['devices'] else 'no device inventory obtained; playback gate closed'
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(dict(report=str(args.report), status=report['status'], devices=len(report['devices']), errors=report['errors'])))


if __name__ == '__main__':
    main()
