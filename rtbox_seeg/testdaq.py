from uldaq import (get_daq_device_inventory, DaqDevice, InterfaceType, AiInputMode, Range, AInFlag)
devices = get_daq_device_inventory(InterfaceType.USB)
