function [onset, output] = feedback(system, t, record)
ideal = t.onset + GetSecs(); % relative time

Screen('DrawTexture', system.w, system.tex.ocean_bottom);
[screenWidth, screenHeight] = Screen('WindowSize', system.w);

% Define the size of the white box (e.g., 100x100 pixels)
boxWidth = 200;
boxHeight = 200;

% Calculate the position of the box in the lower right corner
% The coordinates are in the form [left, top, right, bottom]
boxRect = [screenWidth - boxWidth, screenHeight - boxHeight, screenWidth, screenHeight];

% Define the color
black = [0 0 0];

% Draw the white box
Screen('FillRect', system.w, black, boxRect);


% reach back in time and find the previous choice event
choice = record(t.i-2).output; 
if choice.score
    
    openChest(system, t, record)
    Screen('DrawTexture', system.w, system.tex.ocean_bottom);
    msg = 'REWARD!';
    DrawFormattedText(system.w, msg ,...
        'center','center', [255,255,255]);

    % Load an audio file
    [cash, cashFs] = audioread('out/audio/cash.mp3');

    % Play the audio
    sound(cash, cashFs);
else

    openChest(system, t, record)
    Screen('DrawTexture', system.w, system.tex.ocean_bottom);
    msg = 'NO REWARD!';
    DrawFormattedText(system.w, msg ,...
        'center','center', [255,255,255]);

    % Load an audio file
    [buzz, buzzFs] = audioread('out/audio/buzzer.mp3');

    % Play the audio
    sound(buzz, buzzFs);
end

% TODO: find chosen direction using choice.key
% TODO: show open/closed chest over choice location
% TODO: animate chest sprite

onset = Screen('Flip', system.w, ideal);
output.ideal = ideal;
end
