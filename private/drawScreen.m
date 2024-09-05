
function drawScreen(system, t, color, varargin)

%redraw the scene
ideal = GetSecs()+t.onset;

%% positon choice options
chest_w = 60; chest_h = 60;  %TODO: use sprite
% TODO: use DrawTextures (many at once)

[screenWidth, screenHeight] = Screen('WindowSize', system.w);

% Define the size of the white box (e.g., 100x100 pixels)
boxWidth = 200;
boxHeight = 200;

% Calculate the position of the box in the lower right corner
% The coordinates are in the form [left, top, right, bottom]
boxRect = [screenWidth - boxWidth, screenHeight - boxHeight, screenWidth, screenHeight];

Screen('DrawTexture', system.w, system.tex.ocean_bottom); % Show the background again

% Draw the box
Screen('FillRect', system.w, color, boxRect);

% chest graphics
Screen('DrawTexture', system.w, system.tex.chest_sprites{1,1},...
    [], [ system.pos.left.x system.pos.left.y system.pos.left.x+chest_w system.pos.left.y+chest_h] );
Screen('DrawTexture', system.w, system.tex.chest_sprites{1,1},...
    [], [ system.pos.up.x system.pos.up.y system.pos.up.x+chest_w system.pos.up.y+chest_h] );
Screen('DrawTexture', system.w, system.tex.chest_sprites{1,1},...
    [], [ system.pos.right.x system.pos.right.y system.pos.right.x+chest_w system.pos.right.y+chest_h] );

% add keys to chests
if ismember('right', t.choices)
    Screen('DrawTexture', system.w, system.tex.key,...
        [], [ system.pos.right.x+20 system.pos.right.y+20 system.pos.right.x+chest_w system.pos.right.y+chest_h] );
end

if ismember('left', t.choices)
    Screen('DrawTexture', system.w, system.tex.key,...
        [], [ system.pos.left.x+20 system.pos.left.y+20 system.pos.left.x+chest_w system.pos.left.y+chest_h] );

end

if ismember('up', t.choices)
    Screen('DrawTexture', system.w, system.tex.key,...
        [], [ system.pos.up.x+20 system.pos.up.y+20 system.pos.up.x+chest_w system.pos.up.y+chest_h] );


end

progressBar(system, t)


end
