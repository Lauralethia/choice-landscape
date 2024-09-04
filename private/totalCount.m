function [] = totalCount(system, correctTrials)

% Oval parameters (upper right corner)
[screenWidth, screenHeight] = Screen('WindowSize', system.w);
Screen('TextSize', system.w, 24); % Set text size

ovalWidth = 100;
ovalHeight = 50;
ovalXpos = screenWidth - ovalWidth - 20; % 20 pixels from right edge
ovalYpos = 20;  % 20 pixels from top edge
ovalRect = [ovalXpos, ovalYpos, ovalXpos + ovalWidth, ovalYpos + ovalHeight];


% Draw the oval in the upper right corner
Screen('FillOval', system.w, [255 255 255], ovalRect); % White oval

% Display the correct trial count inside the oval
correctText = sprintf('Total: %d', correctTrials);

% Calculate the text bounding box
textBounds = Screen('TextBounds', system.w, correctText);
textWidth = RectWidth(textBounds);
textHeight = RectHeight(textBounds);

% Center the text horizontally and vertically within the oval
textXpos = ovalXpos + (ovalWidth - textWidth) / 2;
textYpos = (ovalYpos + (ovalHeight - textHeight) / 2)+22;


DrawFormattedText(system.w, correctText, textXpos, textYpos, [0 0 0]);





end