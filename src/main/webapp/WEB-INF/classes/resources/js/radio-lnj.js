{
    const audio = new Audio();

    const formatTime = (totalSeconds, separator) => {
        let minutes = Math.floor(totalSeconds / 60);
        let seconds = Math.floor(totalSeconds % 60);

        // eh, might as well abuse the "JS is not typed" thingy
        if (minutes < 10) minutes = '0' + minutes;
        if (seconds < 10) seconds = '0' + seconds;

        return minutes + separator + seconds;
    };

    {
        // === music progress timer

        let updateTimerTimeoutHandle;

        const updateTimer = () => {
            const progress = audio.currentTime;
            document.getElementById('timer').innerText = formatTime(progress, ' ');

            const millisUntilNextSecond = 1000 - ((progress - Math.floor(progress)) * 1000);
            updateTimerTimeoutHandle = setTimeout(updateTimer, millisUntilNextSecond);
        };

        audio.addEventListener('play', updateTimer);

        audio.addEventListener('pause', () => {
            clearTimeout(updateTimerTimeoutHandle);

            document.getElementById('timer').innerText = '-- --';
            document.getElementById('title').innerText = '--';
        });
    }

    {
        // === playlist handling

        let playlist = [];
        let nextSongTimeoutHandle;
        let playInitiatedByCode = false;

        const getTrackName = () => {
            const length = (playlist[0].duration - 5000) / 1000;
            return playlist[0].trackName + ' (' + formatTime(length, ':') + ')';
        };

        const stopPlaying = () => {
            // actually stop playing the music
            audio.pause();

            // give up on switching to the next song
            clearTimeout(nextSongTimeoutHandle);

            // change "Stop" to "Play"
            document.getElementById('play').removeEventListener('click', stopPlaying);
            document.getElementById('play').addEventListener('click', startPlaying);
            document.getElementById('play').innerText = 'Play';
        };

        const playHeadOfPlaylist = (seekTo) => {
            audio.src = playlist[0].path;
            audio.currentTime = seekTo;
            document.getElementById('title').innerText = getTrackName();
            playInitiatedByCode = true;

            audio.play().catch(e => {
                // display the error to the user and stop the radio
                console.error(e);
                document.getElementById('title').innerText = 'Error starting to play radio: ' + e.message;
                stopPlaying();
            });
        };

        const nextSong = () => {
            // move the finished element to the end of the playlist
            const doneElement = playlist.splice(0, 1);
            playlist.push(doneElement);

            // start playing the next element
            playHeadOfPlaylist(0);

            // prepare switching to the next song
            nextSongTimeoutHandle = setTimeout(nextSong, playlist[0].duration);
        };

        const startPlaying = async function() {
            // load the current progress of the playlist
            const playlistResource = await (await fetch('/radio-lnj/playlist.json')).json();
            playlist = playlistResource.playlist;

            // play the current element
            playHeadOfPlaylist(playlistResource.seek / 1000.);

            // prepare switching to the next song
            nextSongTimeoutHandle = setTimeout(nextSong, playlist[0].duration - playlistResource.seek);

            // change "Play" to "Stop"
            document.getElementById('play').removeEventListener('click', startPlaying);
            document.getElementById('play').addEventListener('click', stopPlaying);
            document.getElementById('play').innerText = 'Stop';
        };

        audio.addEventListener('play', () => {
            if (!playInitiatedByCode) {
                // the user resumed playing, we should catch up with the radio
                stopPlaying();
                startPlaying();
            }

            playInitiatedByCode = false;
        });

        document.getElementById('play').addEventListener('click', startPlaying);
    }
}
