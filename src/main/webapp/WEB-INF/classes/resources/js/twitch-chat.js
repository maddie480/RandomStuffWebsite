{
    // thanks to https://stackoverflow.com/a/8076436
    const hashCode = function (string) {
        let hash = 0;
        for (let i = 0; i < string.length; i++) {
            let code = string.charCodeAt(i);
            hash = ((hash << 5) - hash) + code;
            hash = hash & hash; // Convert to 32bit integer
        }
        return hash;
    }

    const makeMessage = function () {
        const messageDOM = document.createElement('div');
        messageDOM.className = 'standby';
        document.getElementById("body").appendChild(messageDOM);
        return messageDOM;
    };

    let currentMessageDOM = makeMessage();

    const element = function (name, attrs, children = []) {
        const el = document.createElement(name);
        Object.entries(attrs).forEach(attr => el[attr[0]] = attr[1]);
        children.forEach(child => el.appendChild(child));
        return el;
    }

    const expireMessage = function (element) {
        setTimeout(function () {
            element.className = 'leadout';
            setTimeout(function () {
                document.getElementById('body').removeChild(element);
            }, 2000);
        }, 30000);
    }

    const runSocket = function () {
        const socket = new WebSocket('ws://live-chat.maddie480.ovh:11586/');

        socket.onopen = function (e) {
            console.log('Socket connected!');
        }

        socket.onmessage = function (e) {
            const message = JSON.parse(e.data);
            console.log('Message received', message);

            if (message.platform === 'twitch') {
                message.badges.splice(0, 0, 'https://maddie480.ovh/img/twitch.png');
            }
            if (message.platform === 'youtube') {
                message.badges.splice(0, 0, 'https://maddie480.ovh/img/youtube.ico');
            }

            let splicedMessage = [];
            let nextStartIndex = 0;
            message.emotes.sort((a, b) => a.startIndex - b.startIndex);
            message.emotes.forEach(emote => {
                if (nextStartIndex !== emote.startIndex) {
                    splicedMessage.push(element('span', {innerText: message.message.substring(nextStartIndex, emote.startIndex)}));
                }
                splicedMessage.push(element('img', {src: emote.url}));
                nextStartIndex = emote.endIndex;
            });
            if (nextStartIndex !== message.message.length) {
                splicedMessage.push(element('span', {innerText: message.message.substring(nextStartIndex)}));
            }
            console.log("Message spliced into ", splicedMessage);

            currentMessageDOM.appendChild(element('div', {}, [
                element('span', {className: 'author'}, [
                    ...message.badges.map(badge => element('img', {src: badge})),
                    element('span', {className: 'color-' + (Math.abs(hashCode(message.author)) % 15), innerText: message.author})
                ]),
                element('div', {}, [
                    element('span', {className: 'message'}, splicedMessage)
                ])
            ]));

            currentMessageDOM.className = 'shown';
            expireMessage(currentMessageDOM);
            currentMessageDOM = makeMessage();
            socket.send(message.ack);
        }

        socket.onclose = function(event) {
            console.log('Connection closed, wasClean =', event.wasClean);
            setTimeout(runSocket, 5000);
        };
    };

    runSocket();
}
