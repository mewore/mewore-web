package moe.mewore.web.controllers;

import moe.mewore.web.config.ConfigConstants;
import moe.mewore.web.exceptions.NotFoundException;
import moe.mewore.web.services.rabbit.RabbitService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

@RequiredArgsConstructor
@RestController
@RequestMapping(RabbitController.ENDPOINT)
@ResponseBody
@Validated
public class RabbitController {

    public static final String ENDPOINT = ConfigConstants.API_ROOT + "/rabbits";

    private static final String THUMBNAIL_FORMAT_MESSAGE =
            "The correct format is /YYYY-MM-DD/thumbnail.png, where YYYY-MM-DD is the date";

    private static final String RABBIT_DAY_FORMAT_MESSAGE =
            "The correct format is /YYYY-MM-DD/H, where YYYY-MM-DD is the date and H is the hour (from 0 to 23)";

    private final RabbitService rabbitService;

    @GetMapping(path = "/{day}/thumbnail.png", produces = "image/png")
    byte[] getRabbitDayThumbnail(@PathVariable("day") final @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}",
            message = THUMBNAIL_FORMAT_MESSAGE) String day) throws NotFoundException {
        return rabbitService.getDayThumbnail(day);
    }

    @GetMapping(path = "/{day}/{hour}", produces = "image/png")
    byte[] getRabbitHourImage(@PathVariable("day") final @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}",
            message = RABBIT_DAY_FORMAT_MESSAGE) String day,
            @PathVariable("hour") final @Min(value = 0, message = RABBIT_DAY_FORMAT_MESSAGE) @Max(value = 23,
                    message = RABBIT_DAY_FORMAT_MESSAGE) int hour) throws NotFoundException {
        return rabbitService.getDayHourImage(day, hour);
    }
}
