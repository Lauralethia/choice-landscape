import sys
sys.path.insert(0,'/home/abel/luna_habit/RTBox/python') # or PYTHONPATH=~/luna_habit/RTBox/python
import RTBox

box = RTBox.RTBox() # open RTBox, return instance 'box' for later access
box.info()
box.threshold(1)
box.enable('light') # enable light detection
for i in range(10):
    print("waiting for 5 seconds for photodoide")
    box.clear(0)
    (secs, btns) = box.light(5) # wait up to 2 s, secs relative to light
    # enabled = [1, ['press', 'release', 'sound', 'light', 'tr', 'aux']]
    print(f"got it {secs} {btns}")
box.close()


