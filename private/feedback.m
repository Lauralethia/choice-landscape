function [onset, output] = feedback(system, t, record, correctTrials)
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

% Draw the  box
Screen('FillRect', system.w, black, boxRect);

% reach back in time and find the previous choice event
choice = record(t.i-2).output;

if t.i > 3
    correctTrials = record(t.i-4).output.correctTrials; 

end 

if choice.score

    % Load an audio file
    [cash, cashFs] = audioread('out/audio/cash.mp3');

    onset = openChest(system, t, record);
    % Play the audio
    sound(cash, cashFs);
    
    progressBar(system, t);
    correctTrials = correctTrials + 1;
    totalCount(system, correctTrials);
    coinPile(system, correctTrials)
    dropCoin(system,t, correctTrials);



else

    % Load an audio file
    [buzz, buzzFs] = audioread('out/audio/buzzer.mp3');
   
    onset = openChest(system, t, record);
    
    % Play the audio
    sound(buzz, buzzFs);
    progressBar(system, t);
    totalCount(system, correctTrials);
    
   
end

% TODO: find chosen direction using choice.key
% TODO: show open/closed chest over choice location
% TODO: animate chest sprite

output.ideal = ideal;
output.correctTrials = correctTrials; 
end
