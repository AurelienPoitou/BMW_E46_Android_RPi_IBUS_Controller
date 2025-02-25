"""
This module contains the implementation for BluetoothInterface.
"""
import json
import threading
import subprocess
import logging
import time

from bluetooth import advertise_service, BluetoothSocket, RFCOMM, PORT_ANY, SERIAL_PORT_CLASS, SERIAL_PORT_PROFILE

from interfaces.base import BaseInterface

LOGGER = logging.getLogger(__name__)


class BluetoothInterface(BaseInterface):
    """The Bluetooth interface definition for communication with wireless client devices."""

    __interface_name__ = 'bluetooth'

    def __init__(self, controller):
        """
        Initializes a bluetooth service that may be consumed by a remote client.

        Arguments
        ---------
            controller : controllers.base.BaseController
                the parent controller that is instantiated this interface

        """
        super().__init__()
        self.controller = controller
        self.client_sock = None
        self.client_info = None
        self.rfcomm_channel = None
        self.service_name = self.get_setting('service_name')
        self.service_uuid = self.get_setting('service_uuid')
        self.server_sock = None
        self.thread = None
        self.read_buffer_length = self.get_setting('read_buffer_length', int)
        self.is_running = False

    def connect(self):
        """Creates a new thread that listens for an incoming bluetooth RFCOMM connection."""
        LOGGER.info('creating thread for bluetooth interface...')
        self.is_running = True
        self.thread = threading.Thread(target=self.listen_for_rfcomm_connection)
        self.thread.daemon = True
        self.thread.start()

    def listen_for_rfcomm_connection(self):
        """
        Starts bluetooth interfaces and listens for incoming connections.
        """
        while self.is_running:
            try:
                self.perform_hci0_reset()
                # prepare bluetooth server
                self.server_sock = BluetoothSocket(RFCOMM)
                self.server_sock.bind(("", PORT_ANY))
                self.server_sock.listen(1)
                self.rfcomm_channel = self.server_sock.getsockname()[1]

                # start listening for incoming connections
                advertise_service(
                    sock=self.server_sock,
                    name=self.service_name,
                    service_id=self.service_uuid,
                    service_classes=[self.service_uuid, SERIAL_PORT_CLASS],
                    profiles=[SERIAL_PORT_PROFILE]
                )

                LOGGER.info('waiting for connection on RFCOMM channel %d', self.rfcomm_channel)

                # accept received connection
                self.client_sock, self.client_info = self.server_sock.accept()
                self.state = self.__states__.STATE_CONNECTED
                LOGGER.info('accepted connection from %r', self.client_info)

                # start listening for data
                self.consume_bus()
                break
            except Exception:
                LOGGER.exception("[ERROR] failed to establish bluetooth connection, retrying in 10 seconds...")
                time.sleep(10)
            finally:
                if self.server_sock:
                    self.server_sock.close()
                self.server_sock = None

    def disconnect(self):
        """
        Closes Bluetooth connection and resets handle
        """
        LOGGER.info('destroying bluetooth interface...')
        self.is_running = False
        self.state = self.__states__.STATE_DISCONNECTING
        if self.thread:
            self.thread.join(timeout=5)
            self.thread = None
        try:
            if self.client_sock:
                self.client_sock.close()
        except Exception as e:
            LOGGER.error(f"Error closing client socket: {e}")
        finally:
            self.client_sock = None
        try:
            if self.server_sock:
                self.server_sock.close()
        except Exception as e:
            LOGGER.error(f"Error closing server socket: {e}")
        finally:
            self.server_sock = None

        # reset the bluetooth interface
        self.perform_hci0_reset()
        self.state = self.__states__.STATE_READY

    @staticmethod
    def perform_hci0_reset():
        """Resets the bluetooth hci0 device via hciconfig command line interface."""
        try:
            LOGGER.info('performing hci0 down/up...')
            subprocess.run(['hciconfig', 'hci0', 'down'], check=True)
            subprocess.run(['hciconfig', 'hci0', 'up', 'piscan'], check=True)
            LOGGER.info('hci0 down/up has completed')
        except subprocess.CalledProcessError as e:
            LOGGER.exception("Failed to restart hci0 - %r", e)

    def receive(self, data):
        """
        Processes received data from Bluetooth socket

        Arguments
        ---------
            data : bytes
                the data received from the bluetooth connection

        """
        try:
            decoded_data = data.decode('utf-8')
            packet = json.loads(decoded_data)
            LOGGER.info('received packet via bluetooth: %r', packet['data'])

            # invoke bound method (if set)
            if self.receive_hook and callable(self.receive_hook):
                self.receive_hook(packet['data'])

        except json.JSONDecodeError:
            LOGGER.error("Received data is not valid JSON: %r", data)
        except Exception as exception:
            LOGGER.exception('error: %r', exception)

    def send(self, data):
        """
        Sends data via Bluetooth socket connection

        Arguments
        ---------
            data : list
                the data to be sent via this interface (list of IBUSPacket)

        """
        if self.state != self.__states__.STATE_CONNECTED:
            LOGGER.error('error: send() was called but state is not connected')
            return False

        if not self.client_sock:
            LOGGER.error('error: client_sock is not connected')
            self.reconnect()
            return False

        try:
            LOGGER.info('sending IBUSPacket(s)...')
            packets = [packet.as_serializable_dict() for packet in data]

            # encapsulate ibus packets and send
            data_to_send = {"data": json.dumps(packets)}
            LOGGER.debug(f"Sending: {data_to_send}")
            self.client_sock.send(json.dumps(data_to_send))
            return True
        except Exception:
            # socket was closed, graceful restart
            LOGGER.exception('bluetooth send exception')
            self.reconnect()
            return False

    def consume_bus(self):
        """
        Start listening for incoming data
        """
        try:
            LOGGER.info('starting to listen for bluetooth data...')

            while self.is_running and self.client_sock:
                data = self.client_sock.recv(self.read_buffer_length)
                if data:
                    self.receive(data)

        except Exception as exception:
            LOGGER.exception('android device was disconnected - %r', exception)
            self.reconnect()