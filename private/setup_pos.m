function pos = setup_pos(w, varargin);
% TODO: use screen size?
% 2024-08-21 changed x coordinates so that the chests are centered - SM

screens = Screen('Screens');
screenNumber = max(screens);

% Get the size of the screen
[screenWidth, screenHeight] = Screen('WindowSize', screenNumber);


pos.left.x = (screenWidth/2) - 200;
pos.left.y = (screenHeight/2) + 300;

pos.up.x = (screenWidth/2);
pos.up.y = (screenHeight/2);

pos.right.x = (screenWidth/2) + 200;
pos.right.y = (screenHeight/2) + 300;

pos.character.x = (screenWidth/2);
pos.character.y = (screenHeight/2) + 200;

