package maas.infra;

import maas.domain.*;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;

@Component
public class DriverNotificationHateoasProcessor
    implements RepresentationModelProcessor<EntityModel<DriverNotification>> {

    @Override
    public EntityModel<DriverNotification> process(
        EntityModel<DriverNotification> model
    ) {
        return model;
    }
}
