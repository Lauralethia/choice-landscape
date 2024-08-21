function timing = load_events(varargin)
   fprintf('# loading event timing\n');

   % TODO: programaticly set
   % heavy lifting done by .func 
   i = 1;
   timing(i).event_name = 'choice';
   timing(i).func = @choice;
   timing(i).chance = [1,1,1]; % left, up, right
   timing(i).max_rt = 20;
   timing(i).i = i;
   timing(i).choices = ['up','right'];

   i=i+1;
   timing(i).event_name = 'isi';
   timing(i).dur = 4;
   timing(i).cross_color = [0,0,255];%blue
   timing(i).func = @fixation;
   timing(i).onset=0; % as soon as choice ends
   timing(i).i = i;

   i=i+1;
   timing(i).event_name = 'feedback';
   timing(i).dur = 3;
   timing(i).func = @feedback;
   timing(i).onset=timing(i-1).dur;
   timing(i).i = i;

   i=i+1;
   timing(i).event_name = 'iti';
   timing(i).dur = 3;
   timing(i).cross_color = [255,255,255]; % white
   timing(i).func = @fixation;
   timing(i).onset=timing(i-1).dur;
   timing(i).i = i;

end
