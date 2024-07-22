function [onset, output] = choice(system, t, varargin)

   ideal = GetSecs()+t.onset;
   Screen('DrawTexture', system.w, system.tex.ocean_bottom);
   Screen('DrawTexture', system.w, system.tex.astronaut{1,1});

   %% positon choice options
   chest_w = 27;chest_h = 27;  %TODO: use sprite 
   % TODO: use DrawTextures (many at once)
   Screen('DrawTexture', system.w, system.tex.chest,...
          [], [ system.pos.left.x system.pos.left.y system.pos.left.x+chest_w system.pos.left.y+chest_h] );
   Screen('DrawTexture', system.w, system.tex.chest,...
          [], [ system.pos.up.x system.pos.up.y system.pos.up.x+chest_w system.pos.up.y+chest_h] );
   Screen('DrawTexture', system.w, system.tex.chest,...
          [], [ system.pos.right.x system.pos.right.y system.pos.right.x+chest_w system.pos.right.y+chest_h] );

   onset = Screen('Flip', system.w, ideal);

   keys = KbName({'Left','Up', 'Right'});
   [k rt] = waitForKeys(keys, onset + t.max_rt);
   if rt > 0
      idx = find(keys == k,1);
      well_prob = t.chance(idx);
      output.score = (rand(1) <= well_prob);
   else
      output.score = 0
   end
   oupput.onset_ideal = ideal;
   output.key = k;
   output.rt = rt;

   % TODO: animate avatar walk in while loop?
end
