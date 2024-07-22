function timing = load_events(varargin)
   fprintf('# loading event timing\n');

   % TODO: programaticly set
   % heavy lifting done by .func 
   timing(1).event_name = 'instruct1';
   timing(1).func = @(system, varargin) instructions(system, 1, varargin{:});
   timing(1).onset = 0;

   timing(2).event_name = 'iti';
   timing(2).onset = 5;
   timing(2).cross_color = [255,255,255]; % white
   timing(2).func = @fixation;

   timing(3).event_name = 'isi';
   timing(3).onset = 10;
   timing(3).cross_color = [0,0,255];%blue
   timing(3).func = @fixation;
end
