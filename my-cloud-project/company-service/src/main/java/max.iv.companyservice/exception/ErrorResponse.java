package max.iv.companyservice.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorResponse {
    private  long time;
    private String errorMassage;
}
