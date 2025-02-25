"""
This module contains the implementation for IBUSInterface.
"""
import logging
import time
import threading
import serial
import serial.serialutil
from interfaces.base import BaseInterface

LOGGER = logging.getLogger(__name__)


class IBUSInterface(BaseInterface):
    """The IBUS interface definition for communication with BMW vehicles."""

    __interface_name__ = 'ibus'

    def __init__(self, controller):
        """
        Initializes bi-directional communication with IBUS adapter via USB

        Arguments
        ---------
            controller : controllers.base.BaseController
                the parent controller that instantiated this interface

        """
        super().__init__()
        self.controller = controller
        self.baudrate = self.get_setting('baudrate', int)
        self.handle = None
        self.parity = serial.PARITY_EVEN
        self.port = self.get_setting('port')
        self.timeout = self.get_setting('timeout', int)
        self.thread = None
        self.read_buffer_length = self.get_setting('read_buffer_length', int)
        self.is_running = False

    def connect(self):
        """Create daemon thread to establish serial communication."""
        LOGGER.info('creating thread for %s interface...', self.__interface_name__)
        self.is_running = True
        self.thread = threading.Thread(target=self.listen_for_serial_connection)
        self.thread.daemon = True
        self.thread.start()

    def listen_for_serial_connection(self):
        """
        Connect to the vehicle via serial communication.
        """
        while self.is_running:
            try:
                self.state = self.__states__.STATE_CONNECTING
                self.handle = serial.Serial(
                    port=self.port,
                    baudrate=self.baudrate,
                    parity=self.parity,
                    timeout=self.timeout,
                    stopbits=1
                )
                self.state = self.__states__.STATE_CONNECTED
                LOGGER.info('Serial connection established.')
                self.consume_bus()
                break  # Exit the loop if connection is successful
            except serial.serialutil.SerialException as e:
                if "No such file or directory" in str(e):
                    LOGGER.error(f"Failed to open serial port {self.port}: The device file does not exist. "
                                 f"Please ensure the IBUS adapter is connected and the port is correct.")
                else:
                    LOGGER.error(f'Failed to establish serial connection on port {self.port}, retrying in 10 seconds...')
                #self.receive(b'\x80\x05\xbf\x18\x08\x0f\x25\x80\x06\xbf\x19\x0a\x5d\x00\x77\xf0\x03\x68\x01\x9a\x68\x04\xf0\x02\x00\x9e\xd0\x07\xbf\x5b\x23\x00\x04\x00\x14\x3b\x03\x80\x01\xb9\x80\x04\xbf\x02\x00\x39')
                time.sleep(10)
            except FileNotFoundError as e:
                LOGGER.error(f"Failed to open serial port {self.port}: The device file does not exist. "
                             f"Please ensure the IBUS adapter is connected and the port is correct.")
                time.sleep(10)
            except Exception as e:
                LOGGER.error(f"An unexpected error occurred: {e}")
                time.sleep(10)

    def consume_bus(self):
        """Starts an infinite loop on the thread that will continue to read from the bus."""
        try:
            while self.is_running and self.handle and self.handle.is_open:
                data = self.handle.read(self.read_buffer_length)
                if data:
                    self.receive(data)
        except Exception:
            LOGGER.exception('Exception consuming bus, retrying connection in 5 seconds...')
            self.reconnect()

    def disconnect(self):
        """Closes serial connection and resets the handle."""
        LOGGER.info('destroying %s interface...', self.__interface_name__)
        self.is_running = False
        self.state = self.__states__.STATE_DISCONNECTING
        if self.thread:
            self.thread.join(timeout=5)
            self.thread = None
        try:
            if self.handle and self.handle.is_open:
                self.handle.close()
        except Exception as exception:
            LOGGER.exception('Exception during %s disconnect - %r', self.__interface_name__, exception)
        finally:
            self.state = self.__states__.STATE_READY
            self.handle = None

    def receive(self, data):
        """
        Processes bytes received from the interface.

        Arguments
        ---------
            data : bytes
                the bytes retrieved from the bus

        """
        LOGGER.info('bus dump: hex: %r', data.hex())

        packets = []
        bus_dump = bytearray(data)
        packet = bytearray()

        def is_packet_complete():
            """Identifies if packet is complete, i.e. is the size of its length byte.

            Returns
            -------
                bool
                    True if the packet bytearray is a "full/complete" IBUS packet

            """
            if len(packet) < 3:  # At least source_id, length, destination_id
                return False

            length = packet[1]
            entire_length = length + 2  # the source_id and length bytes
            return len(packet) == entire_length

        # process each byte that was received
        for byte in bus_dump:
            packet.append(byte)

            if is_packet_complete():
                try:
                    packet_obj = IBUSPacket(packet)
                    if packet_obj.is_valid():
                        packets.append(packet_obj)
                    else:
                        LOGGER.error('Invalid packet : %r', packet_obj)
                except Exception as e:
                    LOGGER.error(f"Error processing packet: {e}")

                packet = bytearray()  # reset packet

        # invoke bound method (if set)
        if self.receive_hook and callable(self.receive_hook):
            self.receive_hook(packets)

    def send(self, data):
        """
        Writes the provided hex packet(s) to the bus

        Parameters
        ----------
            data : str
                the data to be sent via this interface (hex string)

        """
        if self.state != self.__states__.STATE_CONNECTED:
            LOGGER.error('Error: send() was called but state is not connected')
            return False

        if not self.handle or not self.handle.is_open:
            LOGGER.error('Cannot write to %s interface', self.__interface_name__)
            self.reconnect()
            return False

        try:
            self.handle.write(bytes.fromhex(data))
            return True
        except Exception as e:
            LOGGER.error(f"Error sending data: {e}")
            self.reconnect()
            return False


class IBUSPacket(dict):
    """
    The ibus.packet module represents an IBUS packet that would be processed
    from the connected BUS.

    IBUS Packet
    -----------
        ----------------------------------------------------
        | Source ID | Length | Destination Id | Data | XOR |
        ----------------------------------------------------
                             | ---------- Length ----------|

    """

    def __init__(self, bytes_data):
        """
        Initializes packet object.

        Parameters
        ----------
            bytes_data : bytearray
                the bytearray representation of the ibus packet e.g. '\xFF\x03\x01\x01'

        """
        super().__init__()
        if not isinstance(bytes_data, bytearray):
            raise TypeError("bytes_data must be a bytearray")
        if len(bytes_data) < 3:
            raise ValueError("bytes_data must contain at least 3 bytes")
        self['source_id'] = bytes_data[0]
        self['length'] = bytes_data[1]
        self['destination_id'] = bytes_data[2]

        data_start = 3
        data_end = data_start + self['length'] - 2

        self['data'] = bytes_data[data_start:data_end]
        self['xor_checksum'] = bytes_data[-1]
        self['raw'] = bytes_data
        self['timestamp'] = int(time.time())

    def is_valid(self):
        """Verifies packet information & XOR checksum.

        Returns
        -------
            bool
                True if the packet is valid, False if the packet is invalid

        """
        return self['xor_checksum'] == self.calculate_xor_checksum()

    @staticmethod
    def get_device_name(device_id):
        """Returns the nice human-readable device name for provided device id.

        E.g. 0x50 returns 'MFL Multi FunctionalSteering Wheel Buttons'.

        Arguments
        ---------
            device_id : int
                return the device description for the provided hex code

        Returns
        -------
            basestring
                the name of the device, or "Unknown" if an unrecognized id

        """
        if not isinstance(device_id, int):
            raise TypeError('get_device_name: device_id must be an int')

        device_names = {
            0x00: "Broadcast",
            0x08: "Tilt/Slide Sunroof (SHD)",
            0x10: "Engine Management",
            0x11: "Central Body Electronics (ZKE1, ZKE2)",
            0x12: "Engine Management",
            0x13: "Engine Management",
            0x14: "Engine Management",
            0x15: "Double Sunroof (DDSHD) [E34]",
            0x16: "Thermal Level Oil Sensor [E36]",
            0x18: "CDW - CDC CD-Player",
            0x19: "Rover Automatic Transmission Control Unit",
            0x20: "Electronic Engine Power Control (EML) [M70]",
            0x21: "Central Locking Module [E34, E36]",
            0x22: "Electronic Engine Power Control (EML) [M73]",
            0x23: "?????",
            0x24: "Trunk Lid Module (HKM) [E38]",
            0x28: "Radio Controlled Clock (RCC)",
            0x2e: "Electronic Damper Control (EDC)",
            0x30: "Seat Memory",
            0x31: "MINI EHPR50",
            0x32: "Transmission",
            0x35: "Steering Column Memory (LSM) [E31/E32/E34]",
            0x3b: "NAV Navigation/Video Module",
            0x3f: "Diagnostics",
            0x40: "Remote Control for Central Locking",
            0x43: "Menu Screen",
            0x44: "Drive Away Protection System (EWS)",
            0x45: "Anti-Theft System (DWA)",
            0x46: "Central Information Display (CID) [E83/E85]",
            0x47: "Rear Compartment Monitor (RCM)",
            0x48: "Telephone (Japan)",
            0x50: "MFL Multi Functional Steering Wheel Buttons",
            0x51: "Mirror Memory: Passenger (ZKE5)",
            0x5b: "Automatic Heating/Air Conditioning (IHKA)",
            0x60: "PDC Park Distance Control",
            0x66: "Active Light Control (ALC)",
            0x68: "RAD Radio",
            0x69: "Body Module [E31]",
            0x6a: "DSP Digital Sound Processor",
            0x6b: "Auxiliary Heating 'Webasto' (D-Bus?)",
            0x70: "Tire Pressure Control/Warning (RDC)",
            0x71: "Mirror Memory: Driver (ZKE5)",
            0x72: "Seat Memory: Driver (ZKE5)",
            0x76: "CD Player (Business)",
            0x7f: "Navigation",
            0x80: "IKE Instrument Kombi Electronics",
            0x9a: "Automatic Headlight Vertical Aim Control (LWR)",
            0xa0: "Rear Multi-information Display (MID) [E38]",
            0xa4: "Multiple Restraint System (MRS)",
            0xa7: "Rear Compartment Heating/Air Conditioning",
            0xac: "Electronic Height Control (EHC)",
            0xa8: "?????",
            0xb0: "Speech Recognition System (SES)",
            0xb9: "Compact Remote Control (RF/IR)",
            0xbb: "TV Module",
            0xbf: "LCM Light Control Module",
            0xc0: "MID Multi-Information Display Buttons",
            0xc8: "TEL Telephone",
            0xcd: "Multi Information Display (OBC) [E31]",
            0xd0: "Navigation Location",
            0xda: "Seat Memory: Passenger (ZKE5)",
            0xe0: "Integrated Radio and Information System (IRIS)",
            0xe7: "OBC Text Bar",
            0xe8: "Rain/Driving Light Sensor (RLS)",
            0xea: "DSP Controller [E38]",
            0xed: "Lights, Wipers, Seat Memory",
            0xf0: "BMB Board Monitor Buttons",
            0xf5: "Lamp Control Module [E31]",
            0xff: "Broadcast",
        }

        try:
            return device_names[device_id]
        except KeyError:
            return "Unknown"

    def as_serializable_dict(self):
        """The current android application requires a particular JSON
        structure.

        Python bytearray objects are not JSON serializable, so this function
        will convert it to a list of int.

        Returns
        -------
            dict
                a dictionary of json serializable values

        """
        return {
            'raw': [int(byte) for byte in self['raw']],
            'timestamp': self['timestamp']
        }

    def calculate_xor_checksum(self):
        """Calculates XOR value for packet.

        This is used to verify that the scanned value matches the actual calculated value.

        Returns
        -------
            str
                the checksum value for this packet instance e.g. '\xff'

        """
        checksum = 0

        for key in self['raw'][:-1]:
            checksum = checksum ^ key

        return checksum
