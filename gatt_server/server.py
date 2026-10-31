from bluezero import adapter, peripheral
from gpiozero import LED
import json
import struct
import subprocess
from UUIDS import *

# Value to expose
LED_OFF = 0x00
LED_ON  = 0x01
LED_STATE = [LED_OFF]

test_value = []

# Hardcoded payload streamed over the JSON-chunk protocol (PROTOCOL.md)
JSON_CHUNK_DATA = json.dumps({
    "networks": [
        # Original 18
        {"ssid": "HomeNetwork", "rssi": -45, "secure": True},
        {"ssid": "CoffeeShop", "rssi": -67, "secure": False},
        {"ssid": "Neighbor5G", "rssi": -78, "secure": True},
        {"ssid": "NETGEAR42", "rssi": -52, "secure": True},
        {"ssid": "xfinitywifi", "rssi": -71, "secure": False}
    ]
}).encode('utf-8')


# Offset/chunk size are set by the reset write and consumed by the next reads.
json_chunk_offset = 0
json_chunk_size = 0

# IO
led = LED(17)

char_obj = None  # Direct reference to the characteristic

def read_value():
    print(f"[READ] Characteristic was read: {LED_STATE}")
    return LED_STATE

def get_ip_address():
    try:
        result = subprocess.check_output(['ip', 'addr', 'show', 'wlan0'])
        for line in result.decode().splitlines():
            if 'inet ' in line:
                ip = line.strip().split()[1].split('/')[0]
                return list(ip.encode('utf8'))
    except Exception:
        pass
    return list('0.0.0.0'.encode('utf8'))

def get_ssid():
    try:
        result = subprocess.check_output(['iwgetid', '-r'])
        ssid = result.decode().strip()
        return list(ssid.encode('utf8'))
    except Exception:
        pass
    return list('N/A'.encode('utf8'))

def notify_callback(notifying, characteristic):
    if notifying:
        print("[NOTIFY] Client subscribed to notifications")
    else:
        print("[NOTIFY] Client unsubscribed from notifications")

def write_value(value, options):
    global LED_STATE
    byte_val = bytes(value)
    command = byte_val[0]

    if command not in (LED_OFF, LED_ON):
        raise ValueError(f"[ERROR] Invalid value: {hex(command)}.")

    LED_STATE = [command]
    print(f"[WRITE] Characteristic was written: {LED_STATE}")

    if command == LED_ON:
        led.on()
    elif command == LED_OFF:
        led.off()

    # Emit notification
    if char_obj and char_obj.is_notifying:
        char_obj.set_value(LED_STATE)

def read_test_value():
    print(f"[READ] Test value was read: {test_value}")
    return test_value

def write_test_value(value, options):
    global test_value
    byte_val = bytes(value)
    test_value = byte_val
    print(f"[WRITE] Test value was written: {value}, {byte_val}")

def write_json_chunk_reset(value, options):
    """Reset write: 4 bytes chunk size + 4 bytes offset, both little-endian."""
    global json_chunk_size, json_chunk_offset
    byte_val = bytes(value)

    if len(byte_val) != 8:
        raise ValueError(f"[ERROR] Expected 8 bytes (chunk size + offset), got {len(byte_val)}")

    chunk_size, offset = struct.unpack('<II', byte_val)
    json_chunk_size = chunk_size
    json_chunk_offset = offset
    print(f"[WRITE] JSON chunk reset: chunk_size={chunk_size}, offset={offset}")

def read_json_chunk():
    """Returns the next chunk: 2 bytes current offset + 2 bytes total size + content, all little-endian."""
    global json_chunk_offset

    if json_chunk_size <= 0:
        raise ValueError("[ERROR] Chunk size not set, write chunk size/offset before reading")

    total_size = len(JSON_CHUNK_DATA)
    offset = json_chunk_offset
    content = JSON_CHUNK_DATA[offset:offset + json_chunk_size]

    header = struct.pack('<HH', offset, total_size)
    response = header + content

    json_chunk_offset += len(content)
    print(f"[READ] JSON chunk: offset={offset}, total={total_size}, content_len={len(content)}")
    return list(response)

write_buffer = bytearray()
def write_json_chunk(value, options):
    """Receives JSON chunks: 2 bytes offset + 2 bytes total size + content, little-endian"""
    global write_buffer
    byte_val = bytes(value)

    if len(byte_val) < 4:
        raise ValueError(f"[ERROR] Expected 4 bytes (chunk size + offset), got {len(byte_val)}")

    # I is a 4-byte unsigned int
    # H is a 2-byte unsigned short
    offset, json_total_size = struct.unpack('<HH', byte_val[:4])
    content = byte_val[4:]

    if offset == 0:
        write_buffer = bytearray() # resets buffer

    write_buffer.extend(content)
    print(f"[WRITE] JSON chunk: offset={offset}, total={json_total_size}, content={content}")

    if offset + len(content) == json_total_size:
        json_str = write_buffer.decode('utf-8')
        print(f"[WRITE] JSON complete: {json_str}")

def main():
    global char_obj

    dongle = list(adapter.Adapter.available())[0]
    adapter_address = dongle.address
    print(f"Using adapter: {adapter_address}")

    app = peripheral.Peripheral(
        adapter_address = adapter_address,
        local_name = 'Charlie gatt server'
    )
    app.add_service(
        srv_id = 1,
        uuid = SERVICE_UUID,
        primary = True
    )

    app.add_characteristic(
        srv_id = 1,
        chr_id = 1,
        uuid = CHARACTERISTIC_UUID,
        value = [LED_OFF],
        notifying = False,
        flags = ['read', 'write', 'notify'],  # all three together
        read_callback = read_value,
        write_callback = write_value,
        notify_callback = notify_callback
    )

    app.add_characteristic(
        srv_id = 1,
        chr_id = 2,
        uuid = GET_IP_UUID,
        value = get_ip_address(),
        notifying = False,
        flags = ['read'],
        read_callback = get_ip_address,
        write_callback = None,
        notify_callback = None
    )

    app.add_characteristic(
        srv_id = 1,
        chr_id = 3,
        uuid = GET_SSID_UUID,
        value = get_ssid(),
        notifying = False,
        flags = ['read'],
        read_callback = get_ssid,
        write_callback = None,
        notify_callback = None
    )

    app.add_characteristic(
        srv_id = 1,
        chr_id = 4,
        uuid = TEST_UUID,
        value = test_value,
        notifying = False,
        flags = ['read', 'write'],
        read_callback = read_test_value,
        write_callback = write_test_value,
        notify_callback = None
    )

    app.add_service(
        srv_id = 2,
        uuid = JSON_CHUNK_SERVICE_UUID,
        primary = True
    )

    app.add_characteristic(
        srv_id = 2,
        chr_id = 1,
        uuid = JSON_CHUNK_DATA_UUID,
        value = [],
        notifying = False,
        flags = ['read', 'write'],
        read_callback = read_json_chunk,
        write_callback = write_json_chunk_reset,
        notify_callback = None
    )

    app.add_characteristic(
        srv_id = 2,
        chr_id = 2,
        uuid = WRITE_CHUNK_DATA_UUID,
        value = [],
        notifying = False,
        flags = ['write'],
        write_callback = write_json_chunk,
    )

    char_obj = app.characteristics[0]

    # bluezero puts every `primary=True` service's UUID into the advertisement's
    # ServiceUUIDs field. Two 128-bit UUIDs + the local name overflow BlueZ's
    # advertising payload (this is what caused "Failed to register advertisement").
    # The Android client never scans for JSON_CHUNK_SERVICE_UUID (it only filters on
    # SERVICE_UUID, then discovers the rest via GATT after connecting - see
    # BleScanner.kt), so it's safe to keep it a real primary GATT service while
    # excluding it from the advertised UUID list.
    app.primary_services.remove(JSON_CHUNK_SERVICE_UUID)

    print("GATT server starting")
    print(f"Local name: Charlie gatt server")
    print(f"Service UUID: {SERVICE_UUID}")
    print(f"Char UUID:    {CHARACTERISTIC_UUID}")
    print(f"Value: {LED_STATE}")
    print("Advertising - waiting for connections (Ctrl+c to stop)")
    app.publish()

if __name__ == '__main__':
    main()