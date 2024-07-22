 function send_triggers(hid, TTL)
  if hid > 0
      DaqDOut(hid, 0, TTL);
  end 
end
