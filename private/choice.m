function [onset, output] = choice(system, t, varargin)

keys = [];

ideal = GetSecs()+t.onset;
Screen('DrawTexture', system.w, system.tex.ocean_bottom); % Show the background again
Screen('DrawTexture', system.w, system.tex.astronaut{1,1},...
    [], [system.pos.character.x system.pos.character.y system.pos.character.x+60 system.pos.character.y+80] );

%% positon choice options
chest_w = 40;chest_h = 40;  %TODO: use sprite
% TODO: use DrawTextures (many at once)

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
    keys = [keys, system.keys.right];
end

if ismember('left', t.choices)
    Screen('DrawTexture', system.w, system.tex.key,...
        [], [ system.pos.left.x system.pos.left.y system.pos.left.x+chest_w system.pos.left.y+chest_h] );
    keys = [keys, system.keys.left];

end

if ismember('up', t.choices)
    Screen('DrawTexture', system.w, system.tex.key,...
        [], [ system.pos.up.x system.pos.up.y system.pos.up.x+chest_w system.pos.up.y+chest_h] );
    keys = [keys, system.keys.up];


end


onset = Screen('Flip', system.w, ideal);

[k rt] = waitForKeys(keys, onset + t.max_rt);
if rt > 0
    idx = find(keys == k,1);
    well_prob = t.chance(idx);
    output.score = (rand(1) <= well_prob);
else
    output.score = 0;
end
oupput.onset_ideal = ideal;
output.key = k;
output.rt = rt;

% TODO: animate avatar walk in while loop?
end
