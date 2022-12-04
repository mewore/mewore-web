package moe.mewore.web.controllers;

import lombok.RequiredArgsConstructor;
import moe.mewore.web.exceptions.BadRequestException;
import moe.mewore.web.exceptions.NotFoundException;
import moe.mewore.web.services.rabbit.RabbitService;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.constraints.Pattern;
import java.io.IOException;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Controller
@ResponseBody
@RequestMapping("/rabbits")
@Validated
public class RabbitPageController {

    private final RabbitService rabbitService;

    private static final String MONTH_FORMAT_MESSAGE =
            "The correct format is '?month=YYYY-MM', where YYYY is the year and MM is the month";

    @GetMapping
    String getRabbitPage(
            @RequestParam(value = "month", required = false) final @Nullable @Pattern(regexp = "\\d{4}-\\d{2}",
                    message = MONTH_FORMAT_MESSAGE) String month) throws NotFoundException, IOException {
        final String monthToUse =
                month != null && month.matches("\\d{4}-\\d{2}") && Integer.parseInt(month.split("-")[1]) <= 12
                        ? month
                        : null;
        return rabbitService.getIndexPage(RabbitController.ENDPOINT, monthToUse).collect(Collectors.joining("\n"));
    }
}
