function [onset, output] = moveCharacter(system, t, record, varargin)

ideal = GetSecs()+t.onset;

if t.i < 3
    correctTrials = 0;

else

    correctTrials = record(t.i-3).output.correctTrials;

end


color = [0 0 0];

choice = record(t.i-1).output;
xStep = 10;
if strcmp(choice.pick, 'right')
    distance = (system.pos.right.x-40) - system.pos.character.x;
    steps = distance/xStep;
    for x = 1:steps
        drawScreen(system, t, color)
        totalCount(system, correctTrials);

        xPosition = system.pos.character.x + (x * xStep);

        % Determine the current frame (1, 2, 3, or 4) based on the current step
        currentFrame = mod(x - 1, 4) + 1;

        Screen('DrawTexture', system.w, system.tex.astronaut{currentFrame,3},...
            [], [xPosition system.pos.right.y xPosition+60 system.pos.right.y+80] );
        onset = Screen('Flip', system.w, ideal);
        % Optional: Add a small delay to control the speed of movement
        WaitSecs(0.02);

    end

elseif strcmp(choice.pick, 'left')

    distance = system.pos.character.x - (system.pos.left.x+20) ;
    steps = distance/xStep;
    for x = 1:steps

        drawScreen(system, t, color, varargin)
        totalCount(system, correctTrials);

        xPosition = system.pos.character.x - (x * xStep);

        % Determine the current frame (1, 2, 3, or 4) based on the current step
        currentFrame = mod(x - 1, 4) + 1;

        Screen('DrawTexture', system.w, system.tex.astronaut{currentFrame,2},...
            [], [xPosition system.pos.left.y xPosition+60 system.pos.left.y+80] );
        onset = Screen('Flip', system.w, ideal);
        % Optional: Add a small delay to control the speed of movement
        WaitSecs(0.02);

    end

elseif strcmp(choice.pick, 'up')

    distance = system.pos.character.y - (system.pos.up.y+40);
    steps = distance/xStep;
    for y = 1:steps

        drawScreen(system, t, color, varargin)
        totalCount(system, correctTrials);

        yPosition = system.pos.character.y - (y * xStep);

        % Determine the current frame (1, 2, 3, or 4) based on the current step
        currentFrame = mod(y - 1, 4) + 1;

        Screen('DrawTexture', system.w, system.tex.astronaut{currentFrame,4},...
            [], [system.pos.character.x yPosition system.pos.character.x+60 yPosition+80] );
        onset = Screen('Flip', system.w, ideal);
        % Optional: Add a small delay to control the speed of movement
        WaitSecs(0.02);

    end

else

    drawScreen(system, t, color, varargin)

    Screen('DrawTexture', system.w, system.tex.astronaut{1,1},...
        [], [system.pos.character.x system.pos.character.y system.pos.character.x+60 system.pos.character.y+80] );
    onset = Screen('Flip', system.w, ideal);


end

output.onset_ideal = ideal;
output.onset = onset;
end
