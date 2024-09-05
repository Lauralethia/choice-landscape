function coinPile(system, correctTrials)

[screenWidth, screenHeight] = Screen('WindowSize', system.w);


coinSize = 20; % 20x20 pixels

% Loop through and draw 'c' number of coins based on correct trials

for c = 1:(correctTrials)
    % Calculate position for each coin (spacing coins horizontally)
    xPos = screenWidth / 3 + (c - 1) * (coinSize + 10); % Horizontal spacing
    yPos = screenHeight / 3; % Keep vertical position constant

    % Draw the coin texture at the calculated position
    Screen('DrawTexture', system.w, system.tex.starcoin, [], [xPos yPos xPos + coinSize, yPos + coinSize]);
end
