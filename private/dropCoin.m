function dropCoin(system, t, correctTrials)

[screenWidth, screenHeight] = Screen('WindowSize', system.w);
ideal = GetSecs() + t.onset;

color = [0 0 0];

drawScreen(system, t, color)
totalCount(system, correctTrials);

yStep = 10; % Step size for animating the coin downward
coinSize = 20; % 20x20 pixels

% Parameters
coins_per_row = 15; % Number of coins per row
row_spacing = coinSize + 20; % Vertical spacing between rows
yPos_start = screenHeight / 3; % Initial vertical position
xPos_start = screenWidth / 3; % Initial horizontal position

% Loop through and draw 'c' number of coins based on correct trials
for c = 1:(correctTrials - 1)
    % Calculate row and column positions for each static coin
    row = floor((c - 1) / coins_per_row); % Determine the current row
    col = mod((c - 1), coins_per_row); % Determine the column within the row

    % Update horizontal and vertical positions
    xPos = xPos_start + col * (coinSize + 10); % Horizontal position
    yPos = yPos_start - row * row_spacing; % Vertical position (rows stack upward)

    % Draw the static coin texture at the calculated position
    Screen('DrawTexture', system.w, system.tex.starcoin, [], [xPos, yPos, xPos + coinSize, yPos + coinSize]);
end

if correctTrials >= 1
    % Floating coin's initial Y-position (above the topmost row)
    yStart = 0;

    % Floating coin's final Y-position (end point is where the static coins are)
    new_coin_row = floor((correctTrials - 1) / coins_per_row); % Determine the current row for the new coin
    yEnd = yPos_start - new_coin_row * row_spacing; % Final vertical position for the new coin
    xPosNewCoin = xPos_start + mod((correctTrials - 1), coins_per_row) * (coinSize + 10); % Horizontal position

    % Calculate the distance and the number of steps needed for animation
    distance = yEnd - yStart;
    steps = round(distance / yStep);

    % Animate the new coin floating down in steps
    for x = 1:steps
        drawScreen(system, t, color);
        totalCount(system, correctTrials);

        % Redraw the static coins during each frame of the animation
        for c = 1:(correctTrials - 1)
            row = floor((c - 1) / coins_per_row);
            col = mod((c - 1), coins_per_row);
            xPos = xPos_start + col * (coinSize + 10); % Horizontal position
            yPos = yPos_start - row * row_spacing; % Vertical position

            % Draw the static coins
            Screen('DrawTexture', system.w, system.tex.starcoin, [], [xPos, yPos, xPos + coinSize, yPos + coinSize]);
        end

        % Animate the floating coin down smoothly
        yPositionFloating = yStart + (yEnd - yStart) * (x / steps); % Linear interpolation

        % Draw the floating coin at its current position
        Screen('DrawTexture', system.w, system.tex.starcoin, [], [xPosNewCoin, yPositionFloating, xPosNewCoin + coinSize, yPositionFloating + coinSize]);

        % Flip the screen to show the current frame
        onset = Screen('Flip', system.w);

        % Optional: Add a small delay to control the speed of movement
        WaitSecs(0.02);
    end
end