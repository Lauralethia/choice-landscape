% Port 'habit_seeg' webUI+python bridge to Psychopy
% 20240717WF - init

function info = habit(varargin)
   Screen('CloseAll')

   %% 
   system = load_system(varargin{:});
   timing = load_events(varargin{:});
   system.w = setup_screen(varargin{:});
   system.pos = setup_pos(system.w, varargin{:});
   system.tex = load_textures(system.w, varargin{:});

   %% instructions
   [onset, output] = instructions(system, 1);

   %% start timing and data collection
   record(length(timing)) = struct();
   system.starttime = GetSecs();

   %% run through events
   for i=1:length(timing)
      t = timing(i);
      [onset, output] = t.func(system, t, record);
      record(i).event_name = t.event_name;
      record(i).output = output;
      record(i).onset = onset;
   end

   info.record = record;
   info.system = system;

   closedown();
end

