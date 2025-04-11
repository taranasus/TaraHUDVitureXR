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

## Next Testing Task:
- Adjust the minimap so it's more thematically accurate with the cyberpunk 2077 minimap. Also make it a little more zoomed in I think.



## Feature Ideas:
- Add to the healthStats two red Icons to the left of the clock, one should show the WiFi status, the other should be just a placeholder for now
- Main UI interface that's always up HUD for regular outside usage.
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
- Display the phone battery level as a health bar in the top-left corner of the glassess UI. 
- Boilerplate implementation
- Refactoring of GlassesDisplayService so that it's smaller.
- Test DateTime is displayed in the glasses.
- Implemented Rajdhani-Medium font as the default font for the entire app.
- Test Font has been changed to desired font, both in glasses and in the app.
- Test background service implementation: Verify that the glasses HUD continues to work when the app is in the background and a different app is open. The HUD should remain visible on the glasses even when the phone is showing other applications.
- The signal bar of the top-left UI does not appear to actually show the signal strength fromt he phone. Perhaps it's a bug? Needs investigating.
- Show a minimap on the HUD. The minimap should be displayed in the same location as shown in this image in roughly the same zice https://kagi.com/proxy/01-HUD_res-1920x1080.jpg?c=r3ruTN54uRUfKZ7YQWAyRjrcWRhLwbKbHxR-z9yws1vBMoyguZdt02IJ_DYtUKGHZ-LtcvlFORMi4p4yTKXBIvm_BSb4rpHkbTn7XBjOeAziuDqXFJjVldjTFfXFJPlS

For now it doesn't need to be styalized like in the game, just implement whatever gets us faster to the goal of showing a minimap on the HUD with the user's location at its centre.

You need to follow the implementation plan written down here minimap_implementation_plan.md in order to do this implementation succesfully. 
- I no longer want to support both the 2D and 3D modes inside of the HUD. It's too much duplication of work. The toggle between 2d and 3d modes must be removed. It should always be forced to display in 2D mode. If 3D mode is detected it should be switched back to 2D mode. All 3D mode related code should be deleted from the project. Only 2D mode should be supported.
- Fixed app startup crashes by implementing a solution to disable Android Studio startup agents that were causing issues on certain devices. Added TaraHUDApplication class and StartupAgentHelper to prevent crashes related to fs-verity errors and code_cache/startup_agents issues.
- Move all the code for the top-left UI elements to a different file (called healthStats) or set of files and have that imported as a separate component in the main glasses_display activity.
- The health component has three bars that display data. Top one displays phone signal, middle displays phone battery, lower one doesn't display anything, it used to just be set to a constant hardcoded valu so I can see it's there. However the bottom bar isn't visible in testing anymore. I assume this is because for some reason the hardcoded valu is 0. This needs to be investigated and set the value to something else if it's just a matter of it being set to 0. If that's not the issue then there's a bug somehwere and needs to be fixed. (Fix: Removed duplicate layout in glasses_display.xml and made HealthStats component visible).


## ON HOLD
- Not possible in current version of SDK (1.0.7) but dev team said they're working on it so might be in the next version when it drops. On hold for now.- Make it automatically switch the glasses to be transparent (not opaque) when the app starts.
- NOT POSSIBLE WITHOUT ROOT ACCESS - While the glassess UI still displays when navigating to a different app, as expected, when pressing the power button on the phone the glasses also get turned off, and when pressing the power button on the phone again the glassess get turned on again. The expected behaviour is for the phone screen to turn off while the glassess still remain powered.
