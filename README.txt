=== To download data from a Congress ===

   java -cp 'dist/gtpounder.jar:dist/lib/*' main.Downloader --folder <download_folder> --congress <congress_number>
   - <download_folder>: folder to store the downloaded files
   - <congress_number>: congress number (109 - 112)


=== To process the data ====

   java -cp 'dist/gtpounder.jar:dist/lib/*' main.Processor --folder <download_folder> --congress <congress_number> --processed-folder <process_folder> --addinfo-folder <external_file_folder>
   - <download_folder>: folder that stores the downloaded files
   - <congress_number>: congress number (109 - 112)
   - <process_folder>: folder to store processed data
   - <external_file_folder>: folder that stores external files that contain additional information such as NOMINATE scores of legislators, labeled topics by the Congressional Bill Project using the Policy Agenda Codebook etc
