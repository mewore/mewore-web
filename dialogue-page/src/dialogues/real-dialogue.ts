export const realDialogue = `
# When a user initiates a conversation by touching this NPC for the first time
Greeting:
  - sup... / hm? / .-.? / :# / hi / hello / ° ^ °

  - - How are you?
    - not bad, wbu?
    - - Same # NT
    - pretty good actually
    - - Nice! # NT
    - feeling kinda down tbh
    - - What's wrong?
      - nothing
      - - Nothing...?!
        - yea, nothing
        - - Ok then. # NT
      - ...
      - - ... # NT
    - - That sucks...
      - yeah it does # NT
    - - I hope you feel better soon!
      - thank u # NT

  - - Long time no see!
    - huh? have i seen u before?
    - - Well.. Uhm... e-e # NT
    - - You have! Don't you remember?!
      - no tbh / e-e""" / *shrivels up and hides*
      - - I can't believe this -.-
        - ...sorry... # NT
      - i call bs on that
      - - I'm serious!
        - hmmm... o yea, now i remember
        - - Took you a while
          - yeah, whatever # NT
        - - ^.^
          - heheh # NT
      - - Alright, I lied!
        - heh, i knew it

  - - What?
    - what do u mean what - u greeted me
    - - No, you did!
      - (sigh) ok this interface isnt very intuitive, i know. but anyway, when u touch me, it means ur saying hi
      - - That sounds inappropriate.
        - compared to everything else around here, touching objects is relatively tame if u ask me
        - - Did you just objectify yourself?!
          - im literally an object
          - - Well, yeah. # NT
        - - Fair enough. # NT
      - - Oh, I see. # NT
      - - I don't get it.
        - exactly # NT

  - - Hiiii!!!! ^_^
    - ...! / o.o" # NT
    - o.o is this how you greet everyone
    - - Yes. Yes, it is. # NT
    - - What's wrong with that?
      - just... whatever, it was just weird / u sound like ur high
      - - Alright, whatever. # NT
      - - You got a problem pal?
        - i dont have a problem... pal
        - - Well you sure sound like you do. Anyway... # NT
        - - Whatever... # NT
        - - I'm DONE with this. GOODBYE. -'_'-
          - ok, geez... (sorry i guess)

# No Topic
NT:
  - ...
  - - So, why are you so flat?
    - ...excuse me? / what did u just say / <_<...
    - - I mean you're a paper-thin flexiprim.
      - oh, that... well, i myself am not sure why im a flexiprim tbh # NT
    - - I didn't mean it like that!
      - hmph # NT
    - - Nevermind...
  - - Alright... bye!
    - cya / bye / ttyl / nice meeting ya

# When a user leaves the area without saying bye (from that moment, the conversation begins to expire)
Leaving:
  - bye...? / ... / cya

# When a user returns within range before the conversation has expired
Returning:
  - weba / hi again / so you returned, huh

# When a user replies in a conversation, but they weren't even talking.
# This can happen if the user clicked on an old reply option.
NoConversation:
  - huh? we werent even talking
  - - Oh... Alright. # NT
  - - Ok, let's start talking then.
    - ok. hi...? / alright. weve already started # Greeting
  - did you say that to me?
  - - Yeah.
    - huh... well i dont get it
    - - Scratch that. # NT
    - - Whatever, bye. # Leaving
  - - No.
    - ok
  - - Well I don't see anyone else around here.
    - fair enough # NT

# When a user tries to reply but is too far away
TooFar:
  - whats that? i cant hear u

# When a user replies to an old conversation
WrongState:
  - we werent talking about any o that... / im confused (did u click on an old link or button?)

# When a user replies with an impossible dialogue choice somehow
ImpossibleChoice:
  - ur not allowed to say that / what is this alien tongue ur speaking / huh?


`;
