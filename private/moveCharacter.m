function [onset, output] = moveCharacter(system, t, record, varargin)

%redraw the scene
ideal = GetSecs()+t.onset;
Screen('DrawTexture', system.w, system.tex.ocean_bottom); % Show the background again


%% positon choice options
chest_w = 40;chest_h = 40;  %TODO: use sprite
% TODO: use DrawTextures (many at once)

[screenWidth, screenHeight] = Screen('WindowSize', system.w);

% Define the size of the white box (e.g., 100x100 pixels)
boxWidth = 200;
boxHeight = 200;

% Calculate the position of the box in the lower right corner
% The coordinates are in the form [left, top, right, bottom]
boxRect = [screenWidth - boxWidth, screenHeight - boxHeight, screenWidth, screenHeight];

% Define the color white (white = [255 255 255])
black = [0 0 0];

% Draw the white box
Screen('FillRect', system.w, black, boxRect);

% chest graphics
Screen('DrawTexture', system.w, system.tex.chest,...
    [], [ system.pos.left.x system.pos.left.y system.pos.left.x+chest_w system.pos.left.y+chest_h] );
Screen('DrawTexture', system.w, system.tex.chest,...
    [], [ system.pos.up.x system.pos.up.y system.pos.up.x+chest_w system.pos.up.y+chest_h] );
Screen('DrawTexture', system.w, system.tex.chest,...
    [], [ system.pos.right.x system.pos.right.y system.pos.right.x+chest_w system.pos.right.y+chest_h] );

% add keys to chests
if ismember('right', t.choices)
    Screen('DrawTexture', system.w, system.tex.key,...
        [], [ system.pos.right.x system.pos.right.y system.pos.right.x+chest_w system.pos.right.y+chest_h] );
end

if ismember('left', t.choices)
    Screen('DrawTexture', system.w, system.tex.key,...
        [], [ system.pos.left.x system.pos.left.y system.pos.left.x+chest_w system.pos.left.y+chest_h] );

end

if ismember('up', t.choices)
    Screen('DrawTexture', system.w, system.tex.key,...
        [], [ system.pos.up.x system.pos.up.y system.pos.up.x+chest_w system.pos.up.y+chest_h] );


end

choice = record(t.i-1).output;
if strcmp(choice.pick, 'right')

    Screen('DrawTexture', system.w, system.tex.astronaut{1,1},...
        [], [system.pos.right.x system.pos.right.y system.pos.right.x+60 system.pos.right.y+80] );
    onset = Screen('Flip', system.w, ideal);


elseif strcmp(choice.pick, 'left')

    Screen('DrawTexture', system.w, system.tex.astronaut{1,1},...
        [], [system.pos.left.x system.pos.left.y system.pos.left.x+60 system.pos.left.y+80] );
    onset = Screen('Flip', system.w, ideal);

elseif strcmp(choice.pick, 'up')

    Screen('DrawTexture', system.w, system.tex.astronaut{1,1},...
        [], [system.pos.up.x system.pos.up.y system.pos.up.x+60 system.pos.up.y+80] );
    onset = Screen('Flip', system.w, ideal);

else
    Screen('DrawTexture', system.w, system.tex.astronaut{1,1},...
        [], [system.pos.character.x system.pos.character.y system.pos.character.x+60 system.pos.character.y+80] );
    onset = Screen('Flip', system.w, ideal);


end

output.onset_ideal = ideal;
output.onset = onset;
end
