# Work Tracker

## How to use, step by step

1. Always read the WORKTRACKER.md and README.md files in full
2. Implement the next dev task as requested
3. Make any updates to the README.md file if needed
4. In the WORKTRACKER.md move the task from the "Next Dev Task" list to the "Next Testing Task list.
5. Git add all changes.
6. Git commit, making sure to specify that the change needs testing
7. Git push
8. You are done

## Next Dev Task:
-

## Next Testing Task:
- Display the phone battery level as a health bar in the top-left corner of the glassess UI. Remove the current items displayed on the glassess UI and instead make an interface that looks exactly like the one portraid in this image (/Users/justinpopa/Repos/TaraHUDVirtuoXR/Documentation/TopLeftUI.jpg) with the following changes:
    - Instead of the number "28" that's currently showed in the picture it should show the current time in military format.
    - The red health bar should show the current phone battery percentage
    - The blue slim bar should indecate phone signal strength
    - The two numbers at the end should show the Day and Month. 315 should be the current day, 405 should be the current month in three-letter format (APR, MAY, NOV, etc)
    - To understand a sense of scale of what this UI should look like on the screen, here is the example UI showed in the context of a screen https://kagi.com/proxy/01-HUD_res-1920x1080.jpg?c=r3ruTN54uRUfKZ7YQWAyRjrcWRhLwbKbHxR-z9yws1vBMoyguZdt02IJ_DYtUKGHZ-LtcvlFORMi4p4yTKXBIvm_BSb4rpHkbTn7XBjOeAziuDqXFJjVldjTFfXFJPlS

## Next human tasks:
- Design an official HUD layout to then figure out what features to build into the glassess.

## Feature Ideas:
- Main UI interface that's always up HUD for regular outside usage
- Various different UIs for specialized needs with a main home interface (Just like the menu system in a game)
- Make interface look like Cyberpunk 2077 interface
![Example 1](https://kagi.com/proxy/01-HUD_res-1920x1080.jpg?c=r3ruTN54uRUfKZ7YQWAyRjrcWRhLwbKbHxR-z9yws1vBMoyguZdt02IJ_DYtUKGHZ-LtcvlFORMi4p4yTKXBIvm_BSb4rpHkbTn7XBjOeAziuDqXFJjVldjTFfXFJPlS)
![Example 2](https://kagi.com/proxy/cyberpunk-2077-inventory.png?c=r3ruTN54uRUfKZ7YQWAyRjrcWRhLwbKbHxR-z9yws1uB_4i6PVbzr6ks6OEhVQjOHEeevQkGFDxu_CXxQlR9j6CXEZ0iMgF07OQ5Tcf9YkbUO0eeZW23OP1Esh-dpnEMFieNCfGsWdSjhaPahY7voA%3D%3D)
![Example 3](https://kagi.com/proxy/Cyberpunk-207712292020-013429-95236.jpg?c=Wm3gB90_xO0KDyFYSPobHLotF6fiM7Cgw5qArYgphVg2VIQvgm8tyurnj5qk29uuLvwSwosK_H-oCpkCvQ3b7Prnk9jNYcangX1zMSIbX8qytgNVJczleUJxhzjYA0gk)
![Example 4](https://kagi.com/proxy/omg7z3u0e3p91.jpg?c=MHaoEHf4JA4T1dYEo1CR0X0TUe2ouvSbn8yjRBD1I_nC9ho-4N4vcnXNlOXXk3q9J45pfeetiT5ugwGR9vm_pvbhpHMDb08-TlkMtfqRU4p9HVI_baJZN8l4eE0RJzT9TXUkqMoHZbRFb7ynNgrGoqaFR8YFnhu3Uan0GiGU4C5_KGNIb8JZk5-fc_7fvK0g)
![Example 5](https://kagi.com/proxy/vo82brtkyk491.jpg?c=TklOzPjLPioJ5YMJT75bSoRpDc5CNyG1ip-t0-zqb3GpJjA69-hJwXUeCbIcFHEI)
- Small to-do list widget with like the top 2-3 items of the day remaining to look like mission objectives
- Make a new display mode where, when set to 3D mode, the images for the two eyes are siwtched around (left image to right eye, right image to left eye)
- Pump the phone cameras feeds into the glassess to see what the phone sees
- Real-time audio to text conversion that gets displayed to the user inside the glassess as it happens
- Real-time sending of all text to a central storage location for later processing by an AI
- Ability to take a picture of what the camera feeds on the phon are showing to upload to the central AI location for storage
- Always on AI voice operation that allows the user to control the glassess and their actions by simply speaking them
- Tight AI integration with the glasses and phone features so the AI can operate the phone independently
- Video feed of the phone's main screen to the glassess within the glassess HUD UI so the user can see what's on the phone screen
- Integrate with AI agents that can operate the phone to do actions like navigate to apps and stuff.
- Music widget inside the glassess with waveform cause I like waveforms
- Notifications display
- A little Bluetooth controller with some buttons that fits in one hand in order to control the UI without getting the phone out and simulate that Videogame experience. Needs at least 6 buttson (up,down,left,right,confirm,cancel)

## Completed
- Boilerplate implementation
- Refactoring of GlassesDisplayService so that it's smaller.
- Test DateTime is displayed in the glasses.
- Implemented Rajdhani-Medium font as the default font for the entire app.
- Test Font has been changed to desired font, both in glasses and in the app.
- Test background service implementation: Verify that the glasses HUD continues to work when the app is in the background and a different app is open. The HUD should remain visible on the glasses even when the phone is showing other applications.

## ON HOLD
- Not possible in current version of SDK (1.0.7) but dev team said they're working on it so might be in the next version when it drops. On hold for now.- Make it automatically switch the glasses to be transparent (not opaque) when the app starts.
- NOT POSSIBLE WITHOUT ROOT ACCESS - While the glassess UI still displays when navigating to a different app, as expected, when pressing the power button on the phone the glasses also get turned off, and when pressing the power button on the phone again the glassess get turned on again. The expected behaviour is for the phone screen to turn off while the glassess still remain powered.
