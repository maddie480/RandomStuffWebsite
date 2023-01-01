<a href="/discord-bots" class="btn btn-outline-secondary">&#x2B05; Back to Discord Bots</a>

<h1>Discord Bots&nbsp;&#x2013; Terms and Privacy</h1>

<p>
    These Discord bots are provided with hopes that they are useful, <b>without any warranty</b>.
    They might not be available at all times or taken down. Extended downtimes and incidents will be announced in
    <a href="https://discord.gg/PdyfMaq9Vq" target="_blank">the support server</a>. So check there if you notice a bot is having issues,
    there might be more information about the situation; and feel free to report any bugs you encounter.
</p>

<p>
    Usage logs for the bots (such as which commands are run and how they are interpreted) are kept for 30 days.
    They are only used to diagnose issues with using the bots.
</p>

<p>
    Backups are also taken every week, to help recover from issues that lead to loss of data.
    This means deleted data will also be gone from the backup at most 7 days later.
</p>

<p>
    The bots, as well as this website, are hosted on Google Cloud, in the US.
</p>

<p>
    Data stored by each bot is listed below.
</p>

<h2>Timezone Bot</h2>

<h3>Without roles</h3>

<p>
    When you set your timezone, the bot saves <b>your user ID, the server ID, and the name of your timezone</b>.
    This information is deleted whenever you run <code>/remove-timezone</code>, or after 1 year if you do not use any commands and no one mentions you in <code>/time-for</code>.
</p>

<p>
    If you wish your timezone data to be deleted for all servers you set a timezone in (including the ones you left),
    reach out on <a href="https://discord.gg/PdyfMaq9Vq" target="_blank">the support server</a>.
</p>

<h3>With roles</h3>

<p>
    When you grab a timezone role, the bot saves <b>your user ID, the server ID, and the name of your timezone</b>.
    This information is deleted whenever you remove your timezone role, when the bot is removed from the server, or within a day after you leave the server.
</p>

<p>
    When you use <code>/toggle-times</code> to display the time it is in timezone roles, the bot saves <b>your server ID</b> to keep track of this choice.
    It is removed if you use <code>/toggle-times</code> again, or if you remove the bot from your server.
</p>

<h2>Mod Structure Verifier</h2>

<p>
    The mods you send for verification are <b>not</b> stored.
</p>

<p>
    When using either of the <code>--setup</code> commands to setup a mod verification channel, the bot saves <b>the server ID and the channel ID</b>
    of the channel where it takes place. <code>--setup-fixed-names</code> also saves <b>the assets and map folder names</b> you configured.
    This information is deleted when you run <code>--remove-setup</code>, or within 7 days if the channel is deleted or the bot is kicked from the server.
</p>

<p>
    When verifying a mod, if everything is fine, the bot posts a small "install" embed after it. In this case,
    <b>the message ID of your message and of the bot response to it</b> are saved. This allows the bot to delete the embed if you delete your message.
    This information is deleted when you delete your message, or after 6 months.
</p>

<h2>Games Bot</h2>

<p>
    This bot <b>does not store anything</b>; the game state is actually stored within the message on Discord's end!
</p>

<h2>Custom Slash Commands</h2>

<p>
    <i>The bot owner is not responsible for the names and contents of the commands created by bot users.</i>
    If offensive commands were created on your server, and you need help figuring out what happened,
    reach out on <a href="https://discord.gg/PdyfMaq9Vq" target="_blank">the support server</a>.
</p>

<p>
    When creating a command, the bot saves <b>the server ID, the command name, the command ID, the answer contents and whether the response should be public or not</b>.
    This information is deleted whenever the command is deleted, or within a day after removing the bot from the server.
</p>

<p class="space"></p>