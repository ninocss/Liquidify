"""OpenHome session client; SOAP argument names come from device SCPD."""
import xml.etree.ElementTree as ET
from urllib.parse import urljoin
from linn_probe import fetch, value


class Session:
    def __init__(self, location):
        root = fetch(location)
        base = value(root, 'URLBase') or location
        self.services = {}
        for node in root.findall('.//{*}service'):
            kind = value(node, 'serviceType')
            description = fetch(urljoin(base, value(node, 'SCPDURL')))
            actions = {}
            for action in description.findall('.//{*}action'):
                actions[value(action, 'name')] = [value(a, 'name') for a in
                    action.findall('./{*}argumentList/{*}argument') if value(a, 'direction') == 'in']
            self.services[kind.split(':')[-2]] = (kind, urljoin(base, value(node, 'controlURL')), actions)

    def call(self, service, action, **arguments):
        kind, url, actions = self.services[service]
        if set(arguments) != set(actions[action]):
            raise ValueError('SOAP arguments do not match device SCPD')
        envelope = ET.Element('{http://schemas.xmlsoap.org/soap/envelope/}Envelope')
        body = ET.SubElement(envelope, '{http://schemas.xmlsoap.org/soap/envelope/}Body')
        operation = ET.SubElement(body, '{' + kind + '}' + action)
        for name in actions[action]:
            ET.SubElement(operation, name).text = str(arguments[name])
        response = fetch(url, ET.tostring(envelope, encoding='utf-8'), {
            'Content-Type': 'text/xml; charset="utf-8"', 'SOAPACTION': '"' + kind + '#' + action + '"'}, timeout=20)
        result = response.find('.//{*}' + action + 'Response')
        if result is None:
            raise ValueError('SOAP response missing')
        return {n.tag.split('}')[-1]: n.text or '' for n in result}
