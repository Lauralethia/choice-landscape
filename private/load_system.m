function system = load_system(varargin)
   fprintf('# loading system (daq, TODO: audio)\n');
   system = struct('hid',load_daq(varargin{:}));
   system.keys.up = KbName('UpArrow');
   system.keys.right = KbName('RightArrow');
   system.keys.left = KbName('LeftArrow');

end

function hid = load_daq(varargin)
  hid = -1;
  if any(strcmp(varargin, 'nodaq')), return; end
  try
     hid = DaqFind;
     DaqDOut(hid,0,0);
  catch e
     quitwithout('DAQ TTL triggers', e);
  end
end
