function [onset, output] = choice(system, t, varargin)

allkeys = [system.keys.left system.keys.up system.keys.right];
keys = []; 

ideal = GetSecs()+t.onset;
Screen('DrawTexture', system.w, system.tex.ocean_bottom); % Show the background again
Screen('DrawTexture', system.w, system.tex.astronaut{1,1},...
    [], [system.pos.character.x system.pos.character.y system.pos.character.x+60 system.pos.character.y+80] );

progressBar(system, t)

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
Screen('DrawTexture', system.w, system.tex.chest_sprites{1,1},...
    [], [ system.pos.left.x system.pos.left.y system.pos.left.x+chest_w system.pos.left.y+chest_h] );
Screen('DrawTexture', system.w, system.tex.chest_sprites{1,1},...
    [], [ system.pos.up.x system.pos.up.y system.pos.up.x+chest_w system.pos.up.y+chest_h] );
Screen('DrawTexture', system.w, system.tex.chest_sprites{1,1},...
    [], [ system.pos.right.x system.pos.right.y system.pos.right.x+chest_w system.pos.right.y+chest_h] );

% add keys to chests
if ismember('right', t.choices)
    Screen('DrawTexture', system.w, system.tex.key,...
        [], [ system.pos.right.x+10 system.pos.right.y+10 system.pos.right.x+chest_w system.pos.right.y+chest_h] );
    keys = [keys system.keys.right];

end

if ismember('left', t.choices)
    Screen('DrawTexture', system.w, system.tex.key,...
        [], [ system.pos.left.x+10 system.pos.left.y+10 system.pos.left.x+chest_w system.pos.left.y+chest_h] );
    keys = [keys system.keys.left];


end

if ismember('up', t.choices)
    Screen('DrawTexture', system.w, system.tex.key,...
        [], [ system.pos.up.x+10 system.pos.up.y+10 system.pos.up.x+chest_w system.pos.up.y+chest_h] );
    keys = [keys system.keys.up];


end


onset = Screen('Flip', system.w, ideal);

[k rt] = waitForKeys(keys, onset + t.max_rt);

if rt > 0
    idx = find(allkeys == k,1);
    fprintf('choice %d, key %d',idx, k)
    well_prob = t.chance(idx);
    output.score = (rand(1) <= well_prob);
    if idx == 1
        output.pick = 'left';

    elseif idx == 2
        output.pick = 'up';

    elseif idx == 3
        output.pick = 'right';

    end
else
    output.score = 0;
    output.pick = 'none';
end
output.onset_ideal = ideal;
output.key = k;
output.rt = rt;



% TODO: animate avatar walk in while loop?
end
