package max.iv.companyservice.mapper;

import org.mapstruct.Qualifier;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Qualifier // Говорим MapStruct, что это квалификатор
@Target(ElementType.METHOD) // Применяется к методам
@Retention(RetentionPolicy.CLASS) // Доступен во время компиляции
public @interface SimpleCompanyMapper {
}
