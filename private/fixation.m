function [onset, output] = fixation(system, t, varargin)

   % show the background
   Screen('DrawTexture', system.w, system.tex.ocean_bottom);
   DrawFormattedText(system.w, '+' ,'center','center', t.cross_color);

   %onset = t.onset + system.starttime; % abs time
   ideal = t.onset + GetSecs(); % relative time
   onset = Screen('Flip', system.w, ideal);
   output.onset_ideal = ideal;
   output.onset = onset;
end
