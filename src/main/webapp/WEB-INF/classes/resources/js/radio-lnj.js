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
            if (audio.paused) {
                console.log('[timer] Stopped timer updating from updateTimer');
                return;
            }

            const progress = audio.currentTime;
            document.getElementById('timer').innerText = formatTime(progress, ' ');

            const millisUntilNextSecond = 1000 - ((progress - Math.floor(progress)) * 1000);
            updateTimerTimeoutHandle = setTimeout(updateTimer, millisUntilNextSecond);
        };

        audio.addEventListener('play', () => {
            updateTimer();
            console.log('[timer] Started timer updating');
        });

        audio.addEventListener('pause', () => {
            clearTimeout(updateTimerTimeoutHandle);
            console.log('[timer] Stopped timer updating');

            document.getElementById('timer').innerText = '-- --';
            document.getElementById('title').innerText = '--';
        });
    }

    {
        // === playlist handling

        let playlist = [];
        let nextSongTimeoutHandle;
        let playInitiatedByCode = false;
        let pauseInitiatedByCode = false;

        const getTrackName = () => {
            const length = (playlist[0].duration - 1000) / 1000;
            return playlist[0].trackName + ' (' + formatTime(length, ':') + ')';
        };

        const stopPlaying = () => {
            console.log('[playback] Stopping playback!');

            // actually stop playing the music
            if (!audio.paused) {
                audio.pause();
                pauseInitiatedByCode = true;
            }

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

            if (audio.paused) {
                playInitiatedByCode = true;

                audio.play().catch(e => {
                    // display the error to the user and stop the radio
                    console.error('Error starting to play:', e);
                    stopPlaying();
                    document.getElementById('title').innerText = 'Error starting to play radio: ' + e.message;
                });
            }
        };

        const nextSong = () => {
            // move the finished element to the end of the playlist
            const doneElement = playlist.splice(0, 1);
            playlist.push(doneElement);

            console.log('[playback] Moving to next song! Will move to next one in', playlist[0].duration, 'ms');

            // start playing the next element
            playHeadOfPlaylist(0);

            // prepare switching to the next song
            nextSongTimeoutHandle = setTimeout(nextSong, playlist[0].duration);
        };

        const startPlaying = async function() {
            // load the current progress of the playlist
            const playlistResource = await (await fetch('/radio-lnj/playlist.json')).json();
            playlist = playlistResource.playlist;

            console.log('[playback] Starting playback! With offset', playlistResource.seek,
                'ms, and will move to next song in', playlist[0].duration - playlistResource.seek, 'ms');

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
            if (playInitiatedByCode) {
                console.log('[event] Play event triggered by code');
            } else {
                console.log('[event] Play event triggered by user');
                startPlaying();
            }

            playInitiatedByCode = false;
        });

        audio.addEventListener('pause', () => {
            if (pauseInitiatedByCode) {
                console.log('[event] Pause event initiated by code');
            } else if (audio.currentTime === audio.duration) {
                console.log('[event] Pause event initiated by end of media');
            } else {
                console.log('[event] Pause event initiated by user');
                stopPlaying();
            }

            pauseInitiatedByCode = false;
        });

        document.getElementById('play').addEventListener('click', startPlaying);
    }

    if (navigator.userAgent.includes('Windows')) {
        console.log('Windows detected! Since fonts are weird, I\'m offsetting the timer by a bit.');
        document.getElementById('timer').className = 'windows';
    }

    console.log('DJ Navet est prêt à tout balancer ! Enjoy ~ Maddie');
}
