function coinPile(system, correctTrials)

[screenWidth, screenHeight] = Screen('WindowSize', system.w);


coinSize = 20; % 20x20 pixels

% Loop through and draw 'c' number of coins based on correct trials

% Parameters
coins_per_row = 15; % Number of coins per row
row_spacing = coinSize + 20; % Vertical spacing between rows
yPos_start = screenHeight / 3; % Initial vertical position
xPos_start = screenWidth / 3; % Initial horizontal position

% Redraw the static coins
for c = 1:(correctTrials)
    % Calculate row and column positions
    row = floor((c - 1) / coins_per_row); % Determine the current row
    col = mod((c - 1), coins_per_row); % Determine the column within the row

    % Update horizontal and vertical positions
    xPos = xPos_start + col * (coinSize + 10); % Horizontal position
    yPos = yPos_start - row * row_spacing; % Vertical position based on row number

    % Draw the coin
    Screen('DrawTexture', system.w, system.tex.starcoin, [], [xPos, yPos, xPos + coinSize, yPos + coinSize]);
end
