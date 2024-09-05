function dropCoin(system, t, correctTrials)

[screenWidth, screenHeight] = Screen('WindowSize', system.w);
ideal = GetSecs()+t.onset;

color = [0 0 0];

drawScreen(system, t, color)
totalCount(system, correctTrials);


yStep = 10;

coinSize = 20; % 20x20 pixels

% Loop through and draw 'c' number of coins based on correct trials
for c = 1:(correctTrials-1)
    % Calculate position for each coin (spacing coins horizontally)
    xPos = screenWidth / 3 + (c - 1) * (coinSize + 10); % Horizontal spacing
    yPos = screenHeight / 3; % Keep vertical position constant

    % Draw the coin texture at the calculated position
    Screen('DrawTexture', system.w, system.tex.starcoin, [], [xPos yPos xPos + coinSize, yPos + coinSize]);
end

if correctTrials >= 1

    % Floating coin's initial Y-position (top of the screen)
    yStart = 0;

    % Floating coin's final Y-position (end point is where the static coins are)
    yEnd = screenHeight / 3;

    % Calculate the number of steps needed to animate the coin
    distance = yEnd - yStart;
    steps = round(distance / yStep);

    % Animate the new coin floating down in steps
    for x = 1:steps
        drawScreen(system, t, color)
        totalCount(system, correctTrials);

        % Calculate the current Y-position of the floating coin
        yPosition = yStart + (x * yStep);

        % Redraw the static coins
        for c = 1:(correctTrials - 1)
            % Static coins' positions (spaced horizontally)
            xPos = screenWidth / 3 + (c - 1) * (coinSize + 10); % Horizontal spacing
            yPos = screenHeight / 3; % Keep vertical position constant
            Screen('DrawTexture', system.w, system.tex.starcoin, [], [xPos, yPos, xPos + coinSize, yPos + coinSize]);
        end

        % Calculate the X-position of the new floating coin (last one)
        xPosNewCoin = screenWidth / 3 + (correctTrials - 1) * (coinSize + 10);

        % Draw the floating coin at its current position
        Screen('DrawTexture', system.w, system.tex.starcoin, [], [xPosNewCoin, yPosition, xPosNewCoin + coinSize, yPosition + coinSize]);

        % Flip the screen to show the current frame
        onset = Screen('Flip', system.w);

        % Optional: Add a small delay to control the speed of movement
        WaitSecs(0.02);
    end
end