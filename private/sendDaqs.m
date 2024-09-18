function sendDaqs(system)

%check if system.hid > 0, when > 0, the DAQ is active 
%if not 0, return early 
% send DAQ pulse (DaqDout.m)(250)
% wait secs(0.01) send anthter daq (low)
% DAQ at the very very start 

if system.hid > 0 
    DaqDOut(system.hid,0,0);
    WaitSecs(0.02);
    DaqDOut(system.hid,0,250);

else
    return; 

end
