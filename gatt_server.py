from bluezero import adapter, peripheral
from gpiozero import LED
import subprocess

## UUIDS
SERVICE_UUID            = '290edf15-b540-4e83-83cf-ba647bf4df20'
CHARACTERISTIC_UUID     = '290edf15-b540-4e83-83cf-ba647bf4df21'
GET_IP_UUID   = '290edf15-b540-4e83-83cf-ba647bf4df22'
GET_SSID_UUID = '290edf15-b540-4e83-83cf-ba647bf4df23'
# The write is persisted now, so a later read returns the written bytes.
TEST_UUID = '290edf15-b540-4e83-83cf-ba647bf4df24'

# Value to expose
LED_OFF = 0x00
LED_ON  = 0x01
LED_STATE = [LED_OFF]

test_value = []

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

    char_obj = app.characteristics[0]

    print("GATT server starting")
    print(f"Local name: Charlie gatt server")
    print(f"Service UUID: {SERVICE_UUID}")
    print(f"Char UUID:    {CHARACTERISTIC_UUID}")
    print(f"Value: {LED_STATE}")
    print("Advertising - waiting for connections (Ctrl+c to stop)")
    app.publish()

if __name__ == '__main__':
    main()