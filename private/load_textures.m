function tex_struct = load_textures(w, varargin)
% read images into struct of named textures tex_struct.ocean_bottom == 32
  fprintf('# loading textures\n');
  imgs = [dir('out/imgs/*.png')];

  for f = imgs'
     [~, fname, ~]  = fileparts(f.name);
     [img, ~, alpha] = imread(fullfile(f.folder,f.name));
     img(:,:,4) = alpha;
     % TODO: adjust images to screen size
     tex_struct.(fname) = Screen('MakeTexture',w, img);
  end
  
end
