import numpy as np
# see also: find_events(...,mask=2**17 -256)
# mask=2**17 -256
def correct_ttl(x):
    """mne expects 8bit channel. biosemi has 24 bit. go down to 16 and adjust
    >>> correct_ttl(np.array([16128],dtype='int64')) # first stim ch val
    np.array([0],dtype='int16')
    """
    return x.astype(np.int32) - 127**2 +1
    v = x - 16128 #  np.log2(16128+256)==14
    v[v==65536] = 0 # 65536==2**16
    return v

def ttl_side_info(side_info):
    ##                 10   11  13
    ##                 left up right
    ## left+up=3       13   14
    ## left+right=4    15       17
    ## up+right=5           16  18
    combo=('left+up','left+right','up+right')
    aval_sides=None
    picked=None
    if side_info < 10:
        try:
            aval_sides=combo[side_info%10 -1]
        except: pass
    elif side_info == 13:
        aval_sides='left+up'
        picked='left'
    elif side_info == 14:
        aval_sides='left+up'
        picked='up'
    elif side_info == 15:
        aval_sides='left+right'
        picked='left'
    elif side_info == 17:
        aval_sides='left+right'
        picked='right'
    elif side_info == 16:
        aval_sides='up+right'
        picked='up'
    elif side_info == 18:
        aval_sides='up+right'
        picked='right'
    return(aval_sides, picked)

def aval_sides_only(ttl):
   ttl_m = ttl % 10 - 3
   aval_sides = ['left+up','left+right','right+up' ][ttl_m]
   return aval_sides

def taskttl_event_side(ttl):
    # 128/129 = start/stop
    # iti chose catch timeout waiting feedback   other
    #  10    20    50     70      150      200     230
    # side sum(left=1,up=2,right=3):
    #left+up left+right up+right
    #      3          4        5
    # picked   left   up    right
    #           10 || 11 || 13
    # score = +10
    ttl=int(ttl)
    sides=('left','up','right')
    (ename, aval_sides, picked, score) = [None]*4
    if ttl == 128:
        ename='start'
    elif ttl == 129:
        ename='stop'
    elif ttl >= 10 and ttl < 20: # 10-15
        ename='iti'
        aval_sides = aval_sides_only(ttl)
    elif ttl < 50: # 20 - 50
        ename =  'chose'
        aval_sides = aval_sides_only(ttl)
        ttl_m = ttl % 10 - 3
        aval_sides = ['left','up','right' ][ttl_m]
    elif ttl < 70:
        ename='catch'
        aval_sides, picked = ttl_side_info(ttl - 50)
    elif ttl < 150:
        ename =  'timeout'
        aval_sides, picked = ttl_side_info(ttl - 70)
    elif ttl < 200:
        ename='waiting'
        aval_sides, picked = ttl_side_info(ttl - 150)
        #aval_sides, picked = ttl_side_info(ttl - 150)
    elif ttl < 230:
        ename='feedback'
        rm = 200
        if ttl > 220:
            rm = rm + 10
            score = True
        else:
            score = False
        aval_sides, picked = ttl_side_info(ttl-rm)

    return (ename, aval_sides, picked, score)

def add_stim_corrected(raw):
    raw.load_data()
    stim_raw = raw.pick_channels(['Status']).get_data()
    info = mne.create_info(['StatusCorrected'], raw.info['sfreq'], ['stim'])
    stim_vals = correct_ttl(stim_raw[0]).reshape(stim_raw.shape)
    stim = mne.io.RawArray(stim_vals, info)
    raw.add_channels([stim], force_update_info=True)
