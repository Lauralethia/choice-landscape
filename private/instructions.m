function [onset, output] = instructions(system, number, varargin)
   output.instruction = number;

   % show the background
   Screen('DrawTexture', system.w, system.tex.ocean_bottom);
   %Screen('DrawTexture', system.w, system.tex.chest);
   DrawFormattedText(system.w, 'Push any key to start' ,...
   'center','center', [255,255,255]);

   % run
   onset = Screen('Flip', system.w, 0);
   acceptkeys = KbName({'Space','Left','Up','Right','Return','1'});
   [output.k output.rt] = waitForKeys(acceptkeys,Inf);
end
