function [onset, output] = instructions(system, number, varargin)
output.instruction = number;

acceptkeys = KbName({'space','LeftArrow','UpArrow','RightArrow','Return','1'}); % Define accepted keys

% Instruction 1
Screen('DrawTexture', system.w, system.tex.ocean_bottom); % Show the background
DrawFormattedText(system.w, 'In this task you will be looking for treasure in three treasure chests', ...
    'center', 'center', [255, 255, 255]); % Draw the first instruction
Screen('Flip', system.w); % Display the content on the screen
waitForKeys(acceptkeys, Inf);

% Instruction 2
Screen('Flip', system.w); % Clear the screen
Screen('DrawTexture', system.w, system.tex.ocean_bottom); % Show the background again
DrawFormattedText(system.w, 'Two keys will appear in front of two of the three chests. You can choose between these two chests with keys', ...
    'center', 'center', [255, 255, 255]); % Draw the second instruction
Screen('Flip', system.w); % Display the content on the screen
waitForKeys(acceptkeys, Inf);

% Instruction 3
Screen('Flip', system.w); % Clear the screen
Screen('DrawTexture', system.w, system.tex.ocean_bottom); % Show the background again
DrawFormattedText(system.w, 'The odds that a chest has treasure will be different. Your task is to learn which chest is most likely to have treasure', ...
    'center', 'center', [255, 255, 255]); % Draw the third instruction
Screen('Flip', system.w); % Display the content on the screen
waitForKeys(acceptkeys, Inf);

% Instruction 4
Screen('Flip', system.w); % Clear the screen
Screen('DrawTexture', system.w, system.tex.ocean_bottom); % Show the background again
DrawFormattedText(system.w, 'Push any key to start', ...
    'center', 'center', [255, 255, 255]); % Draw the final instruction
Screen('Flip', system.w); % Display the content on the screen
waitForKeys(acceptkeys, Inf);

% Ready to run the task
onset = Screen('Flip', system.w, 0); % Final screen flip, getting the onset time
acceptkeys = KbName({'space','LeftArrow','UpArrow','RightArrow','Return','1'}); % Define accepted keys

% Wait for a key press to start the task
[output.k, output.rt] = waitForKeys(acceptkeys, Inf);

end
