{
    // fetch all the HTML <td> elements that match the poll choices
    const pollChoiceElements = {};
    const allElements = document.getElementsByTagName('td');
    for (let i = 0; i < allElements.length; i++) {
        const element = allElements[i];
        pollChoiceElements[element.dataset.choice] = element;
    }

    let pollId = -1;

    const refresh = async function () {
        setTimeout(refresh, 5000);

        // refresh the poll
        const results = await (await fetch('/twitch-poll.json')).json();

        // refresh the page if the poll changed
        if (pollId === -1) {
            pollId = results.id;
        }
        if (results.id !== pollId) {
            window.location.reload();
            return;
        }

        const totalAnswerCount = Object.entries(results.answersByUser).length;
        const answerNames = Object.keys(results.answersWithCase);

        for (let i = 0; i < answerNames.length; i++) {
            const answerName = answerNames[i];
            const answerCount = Object.values(results.answersByUser).filter(s => s === answerName).length;
            const percent = totalAnswerCount === 0 ? 0 : Math.round(100.0 * answerCount / totalAnswerCount);

            pollChoiceElements[answerName].querySelector('.quantity').innerText = percent + '% (' + answerCount + (answerCount === 1 ? ' vote' : ' votes') + ')';
            pollChoiceElements[answerName].querySelector('.bar').style.width = percent + '%';
        }
    }

    refresh();
}
