function [onset, output] = feedback(system, t, record)
   ideal = t.onset + GetSecs(); % relative time

   Screen('DrawTexture', system.w, system.tex.ocean_bottom);
   % reach back in time and find the previous choice event
   choice = record(t.i-2).output,
   if choice.score
      msg = 'REWARD!'
   else
      msg = 'NO REWARD!'
   end

   % TODO: find chosen direction using choice.key
   % TODO: show open/closed chest over choice location
   % TODO: animate chest sprite
   DrawFormattedText(system.w, msg ,...
   'center','center', [255,255,255]);

   onset = Screen('Flip', system.w, ideal);
   output.ideal = ideal;
end
