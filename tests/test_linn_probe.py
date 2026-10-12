import socket
import unittest
from unittest.mock import patch
from xml.etree import ElementTree as ET
from tools import linn_probe as probe


class ProbeTests(unittest.TestCase):
    def test_headers_case_and_invalid_packets(self):
        self.assertEqual(probe.headers(b'HTTP/1.1 200 OK\r\nLOCATION: http://192.168.2.1/dev.xml\r\nUsn: uuid:linn::service\r\n')['usn'],'uuid:linn::service')
        self.assertEqual(probe.headers(b'NOTIFY * HTTP/1.1\r\nLOCATION: ignored\r\n'),{})

    def test_duplicate_services_and_ip_change_keep_uuid(self):
        packets=[(b'HTTP/1.1 200 OK\r\nUSN: uuid:linn::one\r\nLOCATION: http://192.168.2.1/dev.xml\r\n',('192.168.2.1',1900)),
                 (b'HTTP/1.1 200 OK\r\nUSN: uuid:linn::two\r\nLOCATION: http://192.168.2.2/dev.xml\r\n',('192.168.2.2',1900))]
        with patch.object(probe.socket,'socket') as factory:
            factory.return_value.__enter__.return_value.recvfrom.side_effect=packets+[socket.timeout()]
            devices=probe.discover(.1)
        self.assertEqual(len(devices),1)
        self.assertEqual(devices['uuid:linn']['address'],'192.168.2.2')

    def test_reject_external_and_credential_urls(self):
        for address in ('https://192.168.2.1/device.xml','http://user:password@192.168.2.1/device.xml'):
            with self.assertRaises(ValueError):probe.local_url(address)
        with patch.object(probe.socket,'getaddrinfo',return_value=[(2,1,6,'',('8.8.8.8',80))]):
            with self.assertRaises(ValueError):probe.local_url('http://device/device.xml')

    def test_inventory_only_uses_read_actions(self):
        description=ET.fromstring('''<root xmlns="urn:schemas-upnp-org:device-1-0"><device>
          <UDN>uuid:linn</UDN><modelName>Majik DSM</modelName><serviceList><service>
          <serviceType>urn:av-openhome-org:service:Playlist:1</serviceType>
          <controlURL>/control</controlURL><SCPDURL>/scpd</SCPDURL><eventSubURL>/event</eventSubURL>
          </service></serviceList></device></root>''')
        scpd=ET.fromstring('<scpd><actionList>'+''.join('<action><name>'+a+'</name></action>' for a in ('Play','Insert','DeleteAll','TracksMax','ProtocolInfo'))+'</actionList></scpd>')
        with patch.object(probe,'local_url',side_effect=lambda url:url),patch.object(probe,'fetch',side_effect=[description,scpd]),patch.object(probe,'soap',return_value={'Value':'1000'}) as soap:
            devices=probe.inventory('http://192.168.2.1/device.xml')
        self.assertEqual(devices[0]['uuid'],'uuid:linn')
        self.assertEqual([call.args[2] for call in soap.call_args_list],['TracksMax','ProtocolInfo'])
