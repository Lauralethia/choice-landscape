function timing = load_events(varargin)

% fprintf('# loading event timing\n');

nblocks = 3;
ntrials = 15;
i = 0; 

for block = 1:nblocks
    for trial = 1:ntrials

        % Randomly select two different options from 'left', 'up', and 'right'
        all_choices = {'left', 'up', 'right'};
        selected_choices = datasample(all_choices, 2, 'Replace', false); % Randomly select 2 options without replacement

        i = i+1;
        timing(i).event_name = 'choice';
        timing(i).func = @choice;
        timing(i).dur = 2;
        
        % Set chance values depending on the block
        if block == 1
            timing(i).chance = [0,1,1]; % left, up, right
        elseif block == 2
            timing(i).chance = [0.5,0.5,1]; % left, up, right
        elseif block == 3
            timing(i).chance = [0.5,1,0.5]; % left, up, right
        end
        
        timing(i).max_rt = timing(i).dur;
        timing(i).i = i;
        timing(i).choices = selected_choices; % Assign the random choices to this trial
       
        if i>1
            timing(i).onset = timing(i-1).onset + timing(i-1).dur; % as soon as choice ends
        else
            timing(i).onset = 0;
        end


        i=i+1;
        timing(i).event_name = 'isi';
        timing(i).dur = .2;
        timing(i).cross_color = [0,0,255]; % blue
        timing(i).func = @moveCharacter;
        timing(i).onset = timing(i-1).onset + timing(i-1).dur; % as soon as choice ends
        timing(i).i = i;

        i=i+1;
        timing(i).event_name = 'feedback';
        timing(i).dur = .1;
        timing(i).func = @feedback;
        timing(i).onset = timing(i-1).onset + timing(i-1).dur;
        timing(i).i = i;

        i=i+1;
        timing(i).event_name = 'iti';
        timing(i).dur = 1;
        timing(i).cross_color = [255,255,255]; % white
        timing(i).func = @fixation;
        timing(i).onset = timing(i-1).onset + timing(i-1).dur;
        timing(i).i = i;

    end
end

end

