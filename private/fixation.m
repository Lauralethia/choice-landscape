function [onset, output] = fixation(system, t, varargin)

% show the background
Screen('DrawTexture', system.w, system.tex.ocean_bottom);
DrawFormattedText(system.w, '+' ,'center','center', t.cross_color);

[screenWidth, screenHeight] = Screen('WindowSize', system.w);

% Define the size of the white box (e.g., 100x100 pixels)
boxWidth = 200;
boxHeight = 200;

% Calculate the position of the box in the lower right corner
% The coordinates are in the form [left, top, right, bottom]
boxRect = [screenWidth - boxWidth, screenHeight - boxHeight, screenWidth, screenHeight];

% Define the color white (white = [255 255 255])
white = [255 255 255];

% Draw the white box
Screen('FillRect', system.w, white, boxRect);


%onset = t.onset + system.starttime; % abs time
ideal = t.onset + GetSecs(); % relative time
onset = Screen('Flip', system.w, ideal);
output.onset_ideal = ideal;
output.onset = onset;
end
