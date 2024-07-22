function quitwithout(failedthing, e)
   % show error
   if nargin >= 2
      disp(e)
   end
   % pompt to continue
   msg = sprintf('continue without %s (0=end [default]|1=continue)? ', failedthing);
   cont_anyway = input(msg);
   if cont_anyway ~= 1
      sca
      error('NO %s, decided to not continue!', failedthing)
   end
end
