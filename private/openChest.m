function [onset] = openChest(system, t, record, varargin)

choice = record(t.i-2).output;
ideal = GetSecs()+t.onset;

color = [0 0 0];
chest_w = 60; chest_h = 60;  %TODO: use sprite

if t.i <= 3
    correctTrials = 0;
else
    correctTrials = record(t.i-4).output.correctTrials;
end

drawScreen(system, t, color, varargin)
coinPile(system, correctTrials)


if strcmp(choice.pick, 'right')
    distance = record(t.i-1).output.xPositionEnd - system.pos.character.x;
    xStep = round(distance/7);
    if choice.score
        for x = 1:7

            drawScreen(system, t, color, varargin)
            coinPile(system, correctTrials)

            xPosition = record(t.i-1).output.xPositionEnd - (x * xStep);

            % Determine the current frame (1, 2, 3, or 4) based on the current step
            currentFrame = mod(x - 1, 4) + 1;

            Screen('DrawTexture', system.w, system.tex.astronaut{currentFrame,2},...
                [], [xPosition system.pos.right.y xPosition+60 system.pos.right.y+80] );

            Screen('DrawTexture', system.w, system.tex.chest_sprites{x,1},...
                [], [ system.pos.right.x system.pos.right.y system.pos.right.x+chest_w system.pos.right.y+chest_h] );

            totalCount(system, correctTrials);

            onset = Screen('Flip', system.w, ideal);

            % Optional: Add a small delay to control the speed of movement
            WaitSecs(0.02);

        end

    else

        for x = 1:7

            drawScreen(system, t, color, varargin)
            coinPile(system, correctTrials)

            xPosition = record(t.i-1).output.xPositionEnd - (x * xStep);

            % Determine the current frame (1, 2, 3, or 4) based on the current step
            currentFrame = mod(x - 1, 4) + 1;

            Screen('DrawTexture', system.w, system.tex.astronaut{currentFrame,2},...
                [], [xPosition system.pos.right.y xPosition+60 system.pos.right.y+80] );

            Screen('DrawTexture', system.w, system.tex.chest_sprites{x,2},...
                [], [ system.pos.right.x system.pos.right.y system.pos.right.x+chest_w system.pos.right.y+chest_h] );

            totalCount(system, correctTrials);

            onset = Screen('Flip', system.w, ideal);

            % Optional: Add a small delay to control the speed of movement
            WaitSecs(0.02);

        end
    end

elseif strcmp(choice.pick, 'left')
    distance = system.pos.character.x - record(t.i-1).output.xPositionEnd;
    xStep = round(distance/7);
    if choice.score
        for x = 1:7

            drawScreen(system, t, color, varargin)
            coinPile(system, correctTrials)

            xPosition = record(t.i-1).output.xPositionEnd + (x * xStep);

            % Determine the current frame (1, 2, 3, or 4) based on the current step
            currentFrame = mod(x - 1, 4) + 1;

            Screen('DrawTexture', system.w, system.tex.astronaut{currentFrame,3},...
                [], [xPosition system.pos.left.y xPosition+60 system.pos.left.y+80] );


            Screen('DrawTexture', system.w, system.tex.chest_sprites{x,1},...
                [], [ system.pos.left.x system.pos.left.y system.pos.left.x+chest_w system.pos.left.y+chest_h] );
            totalCount(system, correctTrials);

            onset = Screen('Flip', system.w, ideal);
            % Optional: Add a small delay to control the speed of movement
            WaitSecs(0.02);


        end

    else
        for x = 1:7

            drawScreen(system, t, color, varargin)
            coinPile(system, correctTrials)


            xPosition = record(t.i-1).output.xPositionEnd + (x * xStep);

            % Determine the current frame (1, 2, 3, or 4) based on the current step
            currentFrame = mod(x - 1, 4) + 1;

            Screen('DrawTexture', system.w, system.tex.astronaut{currentFrame,3},...
                [], [xPosition system.pos.left.y xPosition+60 system.pos.left.y+80] );


            Screen('DrawTexture', system.w, system.tex.chest_sprites{x,2},...
                [], [ system.pos.left.x system.pos.left.y system.pos.left.x+chest_w system.pos.left.y+chest_h] );
            totalCount(system, correctTrials);


            onset = Screen('Flip', system.w, ideal);
            % Optional: Add a small delay to control the speed of movement
            WaitSecs(0.02);

        end

    end


elseif strcmp(choice.pick, 'up')
    distance = system.pos.character.y - record(t.i-1).output.yPositionEnd;
    xStep = distance/7;
    if choice.score

        for x = 1:7

            drawScreen(system, t, color, varargin)
            coinPile(system, correctTrials)


            Screen('DrawTexture', system.w, system.tex.chest_sprites{x,1},...
                [], [ system.pos.up.x system.pos.up.y system.pos.up.x+chest_w system.pos.up.y+chest_h] );

            yPosition = record(t.i-1).output.yPositionEnd + (x * xStep);

            % Determine the current frame (1, 2, 3, or 4) based on the current step
            currentFrame = mod(x - 1, 4) + 1;

            Screen('DrawTexture', system.w, system.tex.astronaut{currentFrame,1},...
                [], [system.pos.character.x yPosition system.pos.character.x+60 yPosition+80] );

            totalCount(system, correctTrials);


            onset = Screen('Flip', system.w, ideal);
            % Optional: Add a small delay to control the speed of movement
            WaitSecs(0.035);

        end

    else

        for x = 1:7

            drawScreen(system, t, color, varargin)
            coinPile(system, correctTrials)

            Screen('DrawTexture', system.w, system.tex.chest_sprites{x,2},...
                [], [ system.pos.up.x system.pos.up.y system.pos.up.x+chest_w system.pos.up.y+chest_h] );

            yPosition = record(t.i-1).output.yPositionEnd + (x * xStep);

            % Determine the current frame (1, 2, 3, or 4) based on the current step
            currentFrame = mod(x - 1, 4) + 1;

            Screen('DrawTexture', system.w, system.tex.astronaut{currentFrame,1},...
                [], [system.pos.character.x yPosition system.pos.character.x+60 yPosition+80] );

            totalCount(system, correctTrials);
            onset = Screen('Flip', system.w, ideal);
            % Optional: Add a small delay to control the speed of movement
            WaitSecs(0.035);

        end

    end

else



    Screen('DrawTexture', system.w, system.tex.astronaut{1,1},...
        [], [system.pos.character.x system.pos.character.y system.pos.character.x+60 system.pos.character.y+80] );
    onset = Screen('Flip', system.w, ideal);


end

end