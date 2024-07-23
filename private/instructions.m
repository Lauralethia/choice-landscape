function [onset, output] = instructions(system, number, varargin)
   output.instruction = number;

   % show the background
   Screen('DrawTexture', system.w, system.tex.ocean_bottom);
   %Screen('DrawTexture', system.w, system.tex.chest);
   DrawFormattedText(system.w, 'Push any key to start' ,...
   'center','center', [255,255,255]);

   % run
   onset = Screen('Flip', system.w, 0);
   acceptkeys = KbName({'space','LeftArrow','UpArrow','RightArrow','Return','1'}); %changed these to add Arrow - seems that pyschotoolbox didnt recognize just "left" -SDM 20240723

    [output.k output.rt] = waitForKeys(acceptkeys,Inf);
 

end    
